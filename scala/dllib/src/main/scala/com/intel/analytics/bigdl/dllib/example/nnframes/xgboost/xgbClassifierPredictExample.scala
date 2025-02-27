/*
 * Copyright 2016 The BigDL Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intel.analytics.bigdl.dllib.examples.nnframes.xgboost

import com.intel.analytics.bigdl.dllib.NNContext
import com.intel.analytics.bigdl.dllib.nnframes.XGBClassifierModel
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType}
object xgbClassifierPredictExample {
  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      println("Usage: program input_path model_path")
      sys.exit(1)
    }
    val input_path = args(0)
    val model_path = args(1)

    val sc = NNContext.initNNContext()
    val spark = SQLContext.getOrCreate(sc)

    val model = XGBClassifierModel.load(model_path)

    val schema = new StructType(Array(
    StructField("sepal length", DoubleType, true),
    StructField("sepal width", DoubleType, true),
    StructField("petal length", DoubleType, true),
    StructField("petal width", DoubleType, true),
    StructField("class", StringType, true)))
    val df = spark.read.schema(schema).csv(input_path)
    val featureVectorAssembler = new VectorAssembler()
      .setInputCols(Array("sepal length", "sepal width", "petal length", "petal width"))
      .setOutputCol("featureAssembledVector")
    val assembledDF = featureVectorAssembler.transform(df)
    import org.apache.spark.sql.functions.{col, udf}
    import org.apache.spark.ml.linalg.Vector
    val asDense = udf((v: Vector) => v.toDense)
    val xgbInput = assembledDF.withColumn("DenseFeatures", asDense(col("featureAssembledVector")))
    model.setFeaturesCol("DenseFeatures")

    val results = model.transform(df)
    results.show()

    sc.stop()
  }
}
