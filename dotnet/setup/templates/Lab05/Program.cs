using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.Azure.Cosmos;

public class Program
{
    private static readonly string _endpointUri = "";
    private static readonly string _primaryKey = "";

    private static readonly string _databaseId = "NutritionDatabase";
    private static readonly string _containerId = "FoodCollection";

    private static CosmosClient _client;

    public static async Task Main(string[] args)
    {
        _client = new CosmosClient(_endpointUri, _primaryKey);
        
        Database database = _client.GetDatabase(_databaseId);
        Container container = database.GetContainer(_containerId);

    }
}
