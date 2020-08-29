using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Azure.Cosmos;
using Shared;

namespace ChangeFeedConsole
{
    class Program
    {
        private static readonly string _endpointUrl = "";
        private static readonly string _primaryKey = "";
        private static readonly string _databaseId = "StoreDatabase";
        private static readonly string _containerId = "CartContainer";
        private static readonly string _destinationContainerId = "CartContainerByState";
        private static CosmosClient _client = new CosmosClient(_endpointUrl, _primaryKey);

        static async Task Main(string[] args)
        {
                Database database = _client.GetDatabase(_databaseId);
                Container container = database.GetContainer(_containerId);
                Container destinationContainer = database.GetContainer(_destinationContainerId);

                //todo: Add lab code here

                Console.WriteLine("Started Change Feed Processor");
                Console.WriteLine("Press any key to stop the processor...");

                Console.ReadKey();

                Console.WriteLine("Stopping Change Feed Processor");

                //todo: Add stop code here
         }
      }
}