using System;
using System.Threading.Tasks;
using Microsoft.Azure.Cosmos;

public class Program
{
    private static readonly string _endpointUri = "";
    private static readonly string _primaryKey = "";
    private static readonly string _databaseId = "NutritionDatabase";
    private static readonly string _containerId = "FoodCollection";
    private static CosmosClient _client = new CosmosClient(_endpointUri, _primaryKey);

    public static async Task Main(string[] args)
    {
        Database database = _client.GetDatabase(_databaseId);
        Container container = database.GetContainer(_containerId);

        ItemResponse<Food> candyResponse = await container.ReadItemAsync<Food>("19130", new PartitionKey("Sweets"));
        Food candy = candyResponse.Resource;
        Console.Out.WriteLine($"Read {candy.Description}");

        //string sqlA = "SELECT f.description, f.manufacturerName, f.servings FROM foods f WHERE f.foodGroup = 'Sweets'";

        string sqlA = "SELECT f.description, f.manufacturerName, f.servings FROM foods f WHERE f.foodGroup = 'Sweets' and IS_DEFINED(f.description) and IS_DEFINED(f.manufacturerName) and IS_DEFINED(f.servings)";

        FeedIterator<Food> queryA = container.GetItemQueryIterator<Food>(new QueryDefinition(sqlA), requestOptions: new QueryRequestOptions
        {
            MaxConcurrency = 1,
            PartitionKey = new PartitionKey("Sweets")
        });

        foreach (Food food in await queryA.ReadNextAsync())
        {
            await Console.Out.WriteLineAsync($"{food.Description} by {food.ManufacturerName}");

            foreach (Serving serving in food.Servings)
            {
                await Console.Out.WriteLineAsync($"\t{serving.Amount} {serving.Description}");
            }
            await Console.Out.WriteLineAsync();
        }

        string sqlB = "SELECT f.id, f.description, f.manufacturerName, f.servings FROM foods f WHERE f.manufacturerName != null";
        FeedIterator<Food> queryB = container.GetItemQueryIterator<Food>(sqlB, requestOptions: new QueryRequestOptions { MaxConcurrency = 5, MaxItemCount = 100 });
        int pageCount = 0;
        while (queryB.HasMoreResults)
        {
            Console.Out.WriteLine($"---Page #{++pageCount:0000}---");
            foreach (var food in await queryB.ReadNextAsync())
            {
                Console.Out.WriteLine($"\t[{food.Id}]\t{food.Description,-20}\t{food.ManufacturerName,-40}");
            }
        }
    }
}
