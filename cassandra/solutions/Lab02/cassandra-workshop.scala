// Databricks notebook source
import org.apache.spark.sql.cassandra._
//Spark connector
import com.datastax.spark.connector._
import com.datastax.spark.connector.cql.CassandraConnector

//CosmosDB library for multiple retry
import com.microsoft.azure.cosmosdb.cassandra
spark.conf.set("spark.cassandra.connection.host","thvankra-cassandra-labs.cassandra.cosmos.azure.com")
spark.conf.set("spark.cassandra.connection.port","10350")
spark.conf.set("spark.cassandra.connection.ssl.enabled","true")
spark.conf.set("spark.cassandra.auth.username","thvankra-cassandra-labs")
spark.conf.set("spark.cassandra.auth.password","LLjTddvPnkyP3Eqfb7ymlgKoQuEQIabqLVXXReLjmIqHwXR4JUdpTReUPqVyn9JJkkdTHoTCjfbxcUBcqBIlIg==")
spark.conf.set("spark.cassandra.connection.factory", "com.microsoft.azure.cosmosdb.cassandra.CosmosDbConnectionFactory")
spark.conf.set("spark.cassandra.output.batch.size.rows", "1")
spark.conf.set("spark.cassandra.connection.connections_per_executor_max", "5")
spark.conf.set("spark.cassandra.output.concurrent.writes", "500")
spark.conf.set("spark.cassandra.concurrent.reads", "100")
spark.conf.set("spark.cassandra.output.batch.grouping.buffer.size", "250")
spark.conf.set("spark.cassandra.connection.keep_alive_ms", "600000000")

// COMMAND ----------

dbutils.fs.unmount("/mnt/NutritionData.json")

// COMMAND ----------

val containerName = "nutritiondata"
val storageAccountName = "cosmosdblabsv3"
val sas = "?sv=2018-03-28&ss=bfqt&srt=sco&sp=rlp&se=2022-01-01T04:55:28Z&st=2019-08-05T20:02:28Z&spr=https&sig=%2FVbismlTQ7INplqo6WfU8o266le72o2bFdZt1Y51PZo%3D"
val config = "fs.azure.sas." + containerName+ "." + storageAccountName + ".blob.core.windows.net"

dbutils.fs.mount(
  source = "wasbs://nutritiondata@cosmosdblabsv3.blob.core.windows.net/NutritionData.json",
  mountPoint = "/mnt/NutritionData.json",
  extraConfigs = Map(config -> sas))

val file_location = "/mnt/NutritionData.json"
val file_type = "json"
val df = spark.read.format(file_type).option("inferSchema", "true").load(file_location)

// COMMAND ----------

import org.apache.spark.sql.functions._

val lowercasedfrenamed = df.withColumnRenamed("id","foodid")
val lowercasedf = lowercasedfrenamed.select(lowercasedfrenamed.columns.map(x => col(x).as(x.toLowerCase)): _*).toDF()
lowercasedf.show()

// COMMAND ----------

lowercasedf.write.format("org.apache.spark.sql.cassandra").options(Map( "table" -> "foodtable", "keyspace" -> "nutritionkeyspace")).save()
