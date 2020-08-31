using System;
using System.Threading.Tasks;
using Microsoft.Azure.Cosmos;
using Microsoft.Azure.Cosmos.Scripts;
using System.Collections.Generic;
using System.Linq;


public class Program
{
    private static readonly string _endpointUri = "<your-endpoint-url>";
    private static readonly string _primaryKey = "<your-primary-key>";
    private static readonly string _databaseId = "NutritionDatabase";
    private static readonly string _containerId = "FoodCollection";
    private static CosmosClient _client = new CosmosClient(_endpointUri, _primaryKey);

    public static async Task Main(string[] args)
    {
        Database database = _client.GetDatabase(_databaseId);
        Container container = database.GetContainer(_containerId);

        List<Food> foods = new Bogus.Faker<Food>()
        .RuleFor(p => p.Id, f => (-1 - f.IndexGlobal).ToString())
        .RuleFor(p => p.Description, f => f.Commerce.ProductName())
        .RuleFor(p => p.ManufacturerName, f => f.Company.CompanyName())
        .RuleFor(p => p.FoodGroup, f => "Energy Bars")
        .Generate(10000);

        int pointer = 0;
        while (pointer < foods.Count)
        {
            var parameters = new dynamic[] { foods.Skip(pointer) };
            StoredProcedureExecuteResponse<int> result = await container.Scripts.ExecuteStoredProcedureAsync<int>("bulkUpload", new PartitionKey("Energy Bars"), parameters);
            pointer += result.Resource;
            await Console.Out.WriteLineAsync($"{pointer} Total Items\t{result.Resource} Items Uploaded in this Iteration");
        }

        Console.WriteLine("Execution paused for verification. Press any key to continue to delete.");
        Console.ReadKey();

        bool resume;
        do
        {
            string query = "SELECT * FROM foods f WHERE f.foodGroup = 'Energy Bars'";
            var parameters = new dynamic[] { query };
            StoredProcedureExecuteResponse<DeleteStatus> result = await container.Scripts.ExecuteStoredProcedureAsync<DeleteStatus>("bulkDelete", new PartitionKey("Energy Bars"), parameters);
            await Console.Out.WriteLineAsync($"Batch Delete Completed.\tDeleted: {result.Resource.Deleted}\tContinue: {result.Resource.Continuation}");
            resume = result.Resource.Continuation;
        }
        while (resume);
    }
}
