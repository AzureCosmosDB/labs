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
    private static readonly string _peopleContainerId = "PeopleCollection";
    private static readonly string _transactionContainerId = "TransactionCollection";

    private static CosmosClient _client = new CosmosClient(_endpointUri, _primaryKey);

    public static async Task Main(string[] args)
    {
        
        Database database = _client.GetDatabase(_databaseId);
        Container peopleContainer = database.GetContainer(_peopleContainerId);
        Container transactionContainer = database.GetContainer(_transactionContainerId);


    }
}
