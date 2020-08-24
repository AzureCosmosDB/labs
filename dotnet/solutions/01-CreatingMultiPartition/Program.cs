using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Microsoft.Azure.Cosmos;

public class Program
{
    private static readonly string _endpointUri = "";
    private static readonly string _primaryKey = "";
    private static CosmosClient _client;

    public static async Task Main(string[] args)
    {

        _client = new CosmosClient(_endpointUri, _primaryKey);

        Database database = await InitializeDatabase(_client, "EntertainmentDatabase");

        Container container = await InitializeContainer(database, "EntertainmentContainer");

        await LoadFoodAndBeverage(container);

        await LoadTelevision(container);

        await LoadMapViews(container);
        
    }

    private static async Task<Database> InitializeDatabase(CosmosClient client, string databaseId)
    {
        Database database = await client.CreateDatabaseIfNotExistsAsync(databaseId);
        await Console.Out.WriteLineAsync($"Database Id:\t{database.Id}");
        return database;
    }

    private static async Task<Container> InitializeContainer(Database database, string containerId)
    {
        IndexingPolicy indexingPolicy = new IndexingPolicy
        {
            IndexingMode = IndexingMode.Consistent,
            Automatic = true,
            IncludedPaths =
            {
                new IncludedPath
                {
                    Path = "/*"
                }
            },
            ExcludedPaths =
            {
                new ExcludedPath
                {
                    Path = "/\"_etag\"/?"
                }
            }
        };

        ContainerProperties containerProperties = new ContainerProperties(containerId, "/type")
        {
            IndexingPolicy = indexingPolicy
        };
        
        Container container = await database.CreateContainerIfNotExistsAsync(containerProperties, 400);
        await Console.Out.WriteLineAsync($"Container Id:\t{container.Id}");
        return container;
    }

    private static async Task LoadFoodAndBeverage(Container container)
    {
        var foodInteractions = new Bogus.Faker<PurchaseFoodOrBeverage>()
            .RuleFor(i => i.id, (fake) => Guid.NewGuid().ToString())
            .RuleFor(i => i.type, (fake) => nameof(PurchaseFoodOrBeverage))
            .RuleFor(i => i.unitPrice, (fake) => Math.Round(fake.Random.Decimal(1.99m, 15.99m), 2))
            .RuleFor(i => i.quantity, (fake) => fake.Random.Number(1, 5))
            .RuleFor(i => i.totalPrice, (fake, user) => Math.Round(user.unitPrice * user.quantity, 2))
            .GenerateLazy(500);

        foreach (var interaction in foodInteractions)
        {
            ItemResponse<PurchaseFoodOrBeverage> result = await container.CreateItemAsync(interaction, new PartitionKey(interaction.type));
            await Console.Out.WriteLineAsync($"Item Created\t{result.Resource.id}");
        }
    }

    private static async Task LoadTelevision(Container container)
    {
        var tvInteractions = new Bogus.Faker<WatchLiveTelevisionChannel>()
            .RuleFor(i => i.id, (fake) => Guid.NewGuid().ToString())
            .RuleFor(i => i.type, (fake) => nameof(WatchLiveTelevisionChannel))
            .RuleFor(i => i.minutesViewed, (fake) => fake.Random.Number(1, 45))
            .RuleFor(i => i.channelName, (fake) => fake.PickRandom(new List<string> { "NEWS-6", "DRAMA-15", "ACTION-12", "DOCUMENTARY-4", "SPORTS-8" }))
            .GenerateLazy(500);

        foreach (var interaction in tvInteractions)
        {
            ItemResponse<WatchLiveTelevisionChannel> result = await container.CreateItemAsync(interaction, new PartitionKey(interaction.type));
            await Console.Out.WriteLineAsync($"Item Created\t{result.Resource.id}");
        }
    }

    private static async Task LoadMapViews(Container container)
    {
        var mapInteractions = new Bogus.Faker<ViewMap>()
            .RuleFor(i => i.id, (fake) => Guid.NewGuid().ToString())
            .RuleFor(i => i.type, (fake) => nameof(ViewMap))
            .RuleFor(i => i.minutesViewed, (fake) => fake.Random.Number(1, 45))
            .GenerateLazy(500);

        foreach (var interaction in mapInteractions)
        {
            ItemResponse<ViewMap> result = await container.CreateItemAsync(interaction);
            await Console.Out.WriteLineAsync($"Item Created\t{result.Resource.id}");
        }
    }
}

public interface IInteraction
{
    string type { get; }
}

public class GeneralInteraction : IInteraction
{
    public string id { get; set; }

    public string type { get; set; }
}

public class PurchaseFoodOrBeverage : IInteraction
{
    public string id { get; set; }
    public decimal unitPrice { get; set; }
    public decimal totalPrice { get; set; }
    public int quantity { get; set; }
    public string type { get; set; }
}

public class ViewMap : IInteraction
{
    public string id { get; set; }
    public int minutesViewed { get; set; }
    public string type { get; set; }
}

public class WatchLiveTelevisionChannel : IInteraction
{
    public string id { get; set; }
    public string channelName { get; set; }
    public int minutesViewed { get; set; }
    public string type { get; set; }
}