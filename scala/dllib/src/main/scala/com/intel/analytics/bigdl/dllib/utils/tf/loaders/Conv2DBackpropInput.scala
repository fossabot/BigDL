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
package com.intel.analytics.bigdl.dllib.utils.tf.loaders

import java.nio.ByteOrder

import com.intel.analytics.bigdl.Module
import com.intel.analytics.bigdl.dllib.nn.abstractnn.{AbstractModule, Activity, DataFormat}
import com.intel.analytics.bigdl.dllib.nn.tf.Conv2DTranspose
import com.intel.analytics.bigdl.dllib.tensor.TensorNumericMath.TensorNumeric
import org.tensorflow.framework.NodeDef

import scala.reflect.ClassTag
import Utils._
import com.intel.analytics.bigdl.dllib.tensor.Tensor
import com.intel.analytics.bigdl.dllib.utils.tf.Context

class Conv2DBackpropInput extends TensorflowOpsLoader {
  override def build[T: ClassTag](nodeDef: NodeDef, byteOrder: ByteOrder,
    context: Context[T])(implicit ev: TensorNumeric[T]): Module[T] = {

    val attributes = nodeDef.getAttrMap
    val (pW, pH) =
      if (getString(attributes, "padding") == "SAME") {
        (-1, -1)
      } else {
        (0, 0)
      }
    val strideList = getIntList(attributes, "strides")
    require(strideList.head == 1, s"not support strides on batch")

    val format = getString(attributes, "data_format")
    val deconv = format match {
      case "NHWC" =>
        require(strideList(3) == 1, s"not support strides on depth")
        val strideW = strideList(1)
        val strideH = strideList(2)
        Conv2DTranspose[T](strideW, strideH, pW, pH, DataFormat.NHWC)

      case "NCHW" =>
        require(strideList(1) == 1, s"not support strides on depth")
        val strideW = strideList(2)
        val strideH = strideList(3)
        Conv2DTranspose[T](strideW, strideH, pW, pH, DataFormat.NCHW)
      case _ =>
        throw new IllegalArgumentException(s"not supported data format: $format")
    }
    deconv.asInstanceOf[AbstractModule[Activity, Activity, T]]
  }
}

