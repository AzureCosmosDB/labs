using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.Azure.Cosmos;

public class Program
{
    private static readonly string _endpointUri = "";
    private static readonly string _primaryKey = "";
    
    private static readonly string _databaseId = "FinancialDatabase";
    private static readonly string _peopleCollectionId = "PeopleCollection";
    private static readonly string _transactionCollectionId = "TransactionCollection";

    private static CosmosClient _client = new CosmosClient(_endpointUri, _primaryKey);

    public static async Task Main(string[] args)
    {
        Database database = _client.GetDatabase(_databaseId);
        Container peopleContainer = database.GetContainer(_peopleCollectionId);
        Container transactionContainer = database.GetContainer(_transactionCollectionId);

        await CreateMember(peopleContainer);
        await CreateTransactions(transactionContainer);
        await QueryTransactions(transactionContainer);
        await QueryMember(peopleContainer);
        await ReadMember(peopleContainer);
        await EstimateThroughput(peopleContainer);
        await UpdateThroughput(peopleContainer);

    }

    private static async Task<double> CreateMember(Container peopleContainer)
    {
        //object member = new Member { id = "example.document", accountHolder = new Bogus.Person() };
        object member = new Member
        {
            accountHolder = new Bogus.Person(),
            relatives = new Family
            {
                spouse = new Bogus.Person(),
                children = Enumerable.Range(0, 4).Select(r => new Bogus.Person())
            }
        };
        ItemResponse<object> response = await peopleContainer.CreateItemAsync(member);
        await Console.Out.WriteLineAsync($"{response.RequestCharge} RU/s");
        return response.RequestCharge;
    }

    private static async Task CreateTransactions(Container transactionContainer)
    {
        var transactions = new Bogus.Faker<Transaction>()
            .RuleFor(t => t.id, (fake) => Guid.NewGuid().ToString())
            .RuleFor(t => t.amount, (fake) => Math.Round(fake.Random.Double(5, 500), 2))
            .RuleFor(t => t.processed, (fake) => fake.Random.Bool(0.6f))
            .RuleFor(t => t.paidBy, (fake) => $"{fake.Name.FirstName().ToLower()}.{fake.Name.LastName().ToLower()}")
            .RuleFor(t => t.costCenter, (fake) => fake.Commerce.Department(1).ToLower())
            .GenerateLazy(10000);

        List<Task<ItemResponse<Transaction>>> tasks = new List<Task<ItemResponse<Transaction>>>();
        foreach (var transaction in transactions)
        {
            Task<ItemResponse<Transaction>> resultTask = transactionContainer.CreateItemAsync(transaction);
            tasks.Add(resultTask);
        }
        Task.WaitAll(tasks.ToArray());
        foreach (var task in tasks)
        {
            await Console.Out.WriteLineAsync($"Item Created\t{task.Result.Resource.id}");
        }
    }

    private static async Task QueryTransactions(Container transactionContainer)
    {
        //string sql = "SELECT TOP 1000 * FROM c WHERE c.processed = true ORDER BY c.amount DESC";
        //string sql = "SELECT * FROM c WHERE c.processed = true";
        //string sql = "SELECT * FROM c";
        string sql = "SELECT c.id FROM c";
        FeedIterator<Transaction> query = transactionContainer.GetItemQueryIterator<Transaction>(sql);
        var result = await query.ReadNextAsync();
        await Console.Out.WriteLineAsync($"Request Charge: {result.RequestCharge} RUs");
    }

    private static async Task QueryTransactions2(Container transactionContainer)
    {
        int maxItemCount = 1000;
        int maxDegreeOfParallelism = -1;
        int maxBufferedItemCount = 50000;

        QueryRequestOptions options = new QueryRequestOptions
        {
            MaxItemCount = maxItemCount,
            MaxBufferedItemCount = maxBufferedItemCount,
            MaxConcurrency = maxDegreeOfParallelism
        };

        await Console.Out.WriteLineAsync($"MaxItemCount:\t{maxItemCount}");
        await Console.Out.WriteLineAsync($"MaxDegreeOfParallelism:\t{maxDegreeOfParallelism}");
        await Console.Out.WriteLineAsync($"MaxBufferedItemCount:\t{maxBufferedItemCount}");

        string sql = "SELECT * FROM c WHERE c.processed = true ORDER BY c.amount DESC";

        Stopwatch timer = Stopwatch.StartNew();

        FeedIterator<Transaction> query = transactionContainer.GetItemQueryIterator<Transaction>(sql, requestOptions: options);
        while (query.HasMoreResults)
        {
            var result = await query.ReadNextAsync();
        }
        timer.Stop();
        await Console.Out.WriteLineAsync($"Elapsed Time:\t{timer.Elapsed.TotalSeconds}");
    }

    private static async Task QueryMember(Container peopleContainer)
    {
        string sql = "SELECT TOP 1 * FROM c WHERE c.id = 'example.document'";
        FeedIterator<object> query = peopleContainer.GetItemQueryIterator<object>(sql);
        FeedResponse<object> queryResponse = await query.ReadNextAsync();
        await Console.Out.WriteLineAsync($"{queryResponse.RequestCharge} RUs");
        await Console.Out.WriteLineAsync($"{queryResponse.Resource.First()}");

        ItemResponse<object> response = await peopleContainer.ReadItemAsync<object>("example.document", new PartitionKey("Koepp"));
        await Console.Out.WriteLineAsync($"{response.RequestCharge} RUs");

        object member = new Member { accountHolder = new Bogus.Person() };
        ItemResponse<object> createResponse = await peopleContainer.CreateItemAsync(member);
        await Console.Out.WriteLineAsync($"{createResponse.RequestCharge} RUs");

        int expectedWritesPerSec = 200;
        int expectedReadsPerSec = 800;

        await Console.Out.WriteLineAsync($"Estimated load: {response.RequestCharge * expectedReadsPerSec + createResponse.RequestCharge * expectedWritesPerSec} RU per sec");
    }

    private static async Task<double> ReadMember(Container peopleContainer)
    {
        ItemResponse<object> response = await peopleContainer.ReadItemAsync<object>("372a9e8e-da22-4f7a-aff8-3a86f31b2934", new PartitionKey("Batz"));
        await Console.Out.WriteLineAsync($"{response.RequestCharge} RU/s");
        return response.RequestCharge;
    }

    private static async Task EstimateThroughput(Container peopleContainer)
    {
        int expectedWritesPerSec = 200;
        int expectedReadsPerSec = 800;

        double writeCost = await CreateMember(peopleContainer);
        double readCost = await ReadMember(peopleContainer);

        await Console.Out.WriteLineAsync($"Estimated load: {writeCost * expectedWritesPerSec + readCost * expectedReadsPerSec} RU/s");
    }

    private static async Task UpdateThroughput(Container peopleContainer)
    {
        ThroughputResponse response = await peopleContainer.ReadThroughputAsync(new RequestOptions());
        int? current = response.Resource.Throughput;
        await Console.Out.WriteLineAsync($"{current} RU per sec");
        await Console.Out.WriteLineAsync($"Minimum allowed: {response.MinThroughput} RU per sec");
        await peopleContainer.ReplaceThroughputAsync(1000);
    }
}
