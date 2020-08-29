﻿using System;
using System.Threading.Tasks;
using Microsoft.Azure.Cosmos;
using Microsoft.Azure.Cosmos.Scripts;
using System.Collections.Generic;
using System.Linq;

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

    }

    private static async Task BulkUpload(Container container)
    {
        List<Food> foods = new Bogus.Faker<Food>()
        .RuleFor(p => p.Id, f => (-1 - f.IndexGlobal).ToString())
        .RuleFor(p => p.Description, f => f.Commerce.ProductName())
        .RuleFor(p => p.ManufacturerName, f => f.Company.CompanyName())
        .RuleFor(p => p.FoodGroup, f => "Energy Bars")
        .Generate(10000);
    }

    private static async Task BulkDelete(Container container)
    {

    }
}
