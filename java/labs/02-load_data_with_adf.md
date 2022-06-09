# Load Data Into Cosmos DB with ADF

In this lab, you will populate an Azure Cosmos DB container from an existing set of data using tools built in to Azure. After importing, you will use the Azure portal to view your imported data.

> Before you start this lab, you will need to create an Azure Cosmos DB database and container that you will use throughout the lab. You will also use the **Azure Data Factory (ADF)** to import existing data into your container.

## Create Azure Cosmos DB Database and Container

*You will now create a database and container within your Azure Cosmos DB account.*

1. On the left side of the portal, click the **Resource groups** link.

    ![Resource groups](../media/03-resource_groups.jpg)

1. In the **Resource groups** blade, locate and select the **cosmoslabs** *Resource Group*.

    ![Lab resource group](../media/03-lab_resource_group.jpg)

1. In the **cosmoslabs** blade, select the **Azure Cosmos DB** account you recently created.

    ![Cosmos resource](../media/03-cosmos_resource.jpg)

1. In the **Azure Cosmos DB** blade, locate and click the **Overview** link on the left side of the blade. At the top click the **Add Container** button.

    ![Add container](../media/03-add_collection.jpg)

1. In the **Add Container** popup, perform the following actions:

    1. In the **Database id** field, select the **Create new** option and enter the value **ImportDatabase**.

    1. Do not check the **Provision database throughput** option.

        > Provisioning throughput for a database allows you to share the throughput among all the containers that belong to that database. Within an Azure Cosmos DB database, you can have a set of containers which shares the throughput as well as containers, which have dedicated throughput.

    1. In the **Container Id** field, enter the value **FoodCollection**.

    1. In the **Partition key** field, enter the value ``/foodGroup``.

    1. In the **Throughput** field, enter the value ``11000``.

    1. Click the **OK** button.

1. Wait for the creation of the new **database** and **container** to finish before moving on with this lab.

## Import Lab Data Into Container

You will use **Azure Data Factory (ADF)** to import the JSON array stored in the **nutrition.json** file from Azure Blob Storage. If you are completing the lab through Microsoft Hands-on Labs, you can use the pre-created Data Factory within your resource group. You do not need to do Steps 1-4 in this section and can proceed to Step 5 by opening your Data Factory (named importNutritionData with a random number suffix).

1. On the left side of the portal, click the **Resource groups** link.

    > To learn more about copying data to Cosmos DB with ADF, please read [ADF's documentation](https://docs.microsoft.com/en-us/azure/data-factory/connector-azure-cosmos-db). 

    ![Resource groups](../media/03-resource_groups.jpg)

1. In the **Resource groups** blade, locate and select the **cosmoslabs** *Resource Group*.

1. Click **Add** to add a new resource

    ![Add adf](../media/03-add_adf.jpg)

1. Search for **Data Factory** and select it. Create a new **Data Factory**. You should name this data factory **importnutritiondata** with a unique number appended and select the relevant Azure subscription. You should ensure your existing **cosmoslabs** resource group is selected as well as a Version **V2**. Select **East US** as the region. Do not select **Enable GIT** (this may be checked by default). Click **create**.

    ![df](../media/03-adf_selections.jpg)

1. After creation, open your newly created Data Factory. Select **Author & Monitor** and you will launch ADF. You should see a screen similar to the screenshot below. Select **Copy Data**. We will be using ADF for a one-time copy of data from a source JSON file on Azure Blob Storage to a database in Cosmos DB’s SQL API. ADF can also be used for more frequent data transfers from Cosmos DB to other data stores.
    ![](../media/03-adf_author&monitor.jpg)
    ![](../media/03-adf_copydata.jpg)

1. Edit basic properties for this data copy. You should name the task **ImportNutrition** and select to **Run once now**. Do not select **enable git**.

   ![adf-properties](../media/03-adf_properties.jpg)

1. **Create a new connection** and select **Azure Blob Storage**. We will import data from a json file on Azure Blob Storage. In addition to Blob Storage, you can use ADF to migrate from a wide variety of sources. We will not cover migration from these sources in this tutorial.

    ![](../media/03-adf_blob.jpg)

    ![](../media/03-adf_blob2.jpg)

1. Name the source **NutritionJson** and select **SAS URI** as the Authentication method. Please use the following SAS URI for read-only access to this Blob Storage container: 

    `https://cosmosdblabsv3.blob.core.windows.net/?sv=2018-03-28&ss=bfqt&srt=sco&sp=rlp&se=2022-01-01T04:55:28Z&st=2019-08-05T20:02:28Z&spr=https&sig=%2FVbismlTQ7INplqo6WfU8o266le72o2bFdZt1Y51PZo%3D`

    ![](../media/03-adf_connecttoblob.jpg)

1. Click **Next** and then **Browse** to select the **nutritiondata** folder. Then select **NutritionData.json**.

    ![](../media/03-adf_choosestudents.jpg)

1. Do not check **Copy file recursively** or **Binary Copy**. Also ensure that other fields are empty.

    ![](../media/03-adf_source_next.jpg)

1. Select the file format as **JSON format**. Then select **Next**.

    ![](../media/03-adf_source_dataset_format.jpg)

1. You have now successfully connected the Blob Storage container with the nutrition.json file as the source.

1. For the **Destination data store** add the Cosmos DB target data store by selecting **Create new connection** and selecting **Azure Cosmos DB (SQL API)**.

    ![](../media/03-adf_selecttarget.jpg)

1. Name the linked service **targetcosmosdb** and select your Azure subscription and Cosmos DB account. You should also select the Cosmos DB **ImportDatabase** that you created earlier.

    ![](../media/03-adf_selecttargetdb.jpg)

1. Select your newly created **targetcosmosdb** connection as the Destination date store.

    ![](../media/03-adf_destconnectionnext.jpg)

1. Select your **FoodCollection** container from the drop-down menu. You will map your Blob storage file to the correct Cosmos DB container. Click **Next** to continue.

    ![](../media/03-adf_correcttable.jpg)

1. Click through this screen.

    ![](../media/03-adf_destinationconnectionfinal.jpg)

1. There is no need to change any settings. Click **next**.

    ![](../media/03-adf_settings.jpg)

1. Click **Next** to begin deployment After deployment is complete, select **Monitor**.

    ![](../media/03-adf_deployment.jpg)

1. After a few minutes, refresh the page and the status for the ImportNutrition pipeline should be listed as **Succeeded**.

1. Once the import process has completed, close the ADF. You will now proceed to validate your imported data. 

## Validate Imported Data

*The Azure Cosmos DB Data Explorer allows you to view documents and run queries directly within the Azure Portal. In this exercise, you will use the Data Explorer to view the data stored in our container.*

*You will validate that the data was successfully imported into your container using the **Items** view in the **Data Explorer**.*

1. Return to the **Azure Portal** (<http://portal.azure.com>).

1. On the left side of the portal, click the **Resource groups** link.

    ![Resource groups](../media/03-resource_groups.jpg)

1. In the **Resource groups** blade, locate and select the **cosmoslabs** *Resource Group*.

    ![Lab resource group](../media/03-lab_resource_group.jpg)

1. In the **cosmoslabs** blade, select the **Azure Cosmos DB** account you recently created.

    ![Cosmos resource](../media/03-cosmos_resource.jpg)

1. In the **Azure Cosmos DB** blade, locate and click the **Data Explorer** link on the left side of the blade.

    ![Data Explorer pane](../media/03-data_explorer_pane.jpg)

1. In the **Data Explorer** section, expand the **ImportDatabase** database node and then expand the **FoodCollection** container node. 

    ![Container node](../media/03-collection_node.jpg)

1. Within the **FoodCollection** node, click the **Items** link to view a subset of the various documents in the container. Select a few of the documents and observe the properties and structure of the documents.

    ![Documents](../media/03-documents.jpg)

    ![Example document](../media/03-example_document.jpg)
