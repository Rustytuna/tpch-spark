package main.scala

import org.apache.spark.sql.functions.countDistinct
import org.apache.spark.sql.functions.max
import org.apache.spark.sql.functions.count
import org.apache.spark.sql.functions.udf

/**
 * TPC-H Query 21
 * Savvas Savvides <ssavvides@us.ibm.com>
 *
 */
class Q21 extends TpchQuery {

  import sqlContext.implicits._

  override def execute(): Unit = {

    val fsupplier = supplier.select($"s_suppkey", $"s_nationkey", $"s_name")

    val plineitem = lineitem.select($"l_suppkey", $"l_orderkey", $"l_receiptdate", $"l_commitdate")
    //cache

    val flineitem = plineitem.filter($"l_receiptdate" > $"l_commitdate")
    // cache

    val line1 = plineitem.groupBy($"l_orderkey")
      .agg(countDistinct($"l_suppkey").as("suppkey_count"), max($"l_suppkey").as("suppkey_max"))
      .select($"l_orderkey".as("key"), $"suppkey_count", $"suppkey_max")

    val line2 = flineitem.groupBy($"l_orderkey")
      .agg(countDistinct($"l_suppkey").as("suppkey_count"), max($"l_suppkey").as("suppkey_max"))
      .select($"l_orderkey".as("key"), $"suppkey_count", $"suppkey_max")

    val forder = order.select($"o_orderkey", $"o_orderstatus")
      .filter($"o_orderstatus" === "F")

    val res = nation.filter($"n_name" === "SAUDI ARABIA")
      .join(fsupplier, $"n_nationkey" === fsupplier("s_nationkey"))
      .join(flineitem, $"s_suppkey" === flineitem("l_suppkey"))
      .join(forder, $"l_orderkey" === forder("o_orderkey"))
      .join(line1, $"l_orderkey" === line1("key"))
      .filter($"suppkey_count" > 1 || ($"suppkey_count" == 1 && $"l_suppkey" == $"max_suppkey"))
      .select($"s_name", $"l_orderkey", $"l_suppkey")
      .join(line2, $"l_orderkey" === line2("key"), "left_outer")
      .select($"s_name", $"l_orderkey", $"l_suppkey", $"suppkey_count", $"suppkey_max")
      .filter($"suppkey_count" === 1 && $"l_suppkey" === $"suppkey_max")
      .groupBy($"s_name")
      .agg(count($"l_suppkey").as("numwait"))
      .sort($"numwait".desc, $"s_name")
      .limit(100)

    /*

    val flineitem = lineitem.select($"l_suppkey", $"l_orderkey", $"l_receiptdate", $"l_commitdate")
      .cache

    val atLeast1 = flineitem.select($"l_orderkey", $"l_suppkey")
      .groupBy($"l_orderkey")
      .agg(countDistinct($"l_suppkey").as("suppkey_count"))
      .filter($"suppkey_count" > 1)
      .select($"l_orderkey".as("key"))

    val exactly1 = flineitem.filter($"l_receiptdate" > $"l_commitdate")
      .select($"l_orderkey", $"l_suppkey")
      .join(atLeast1, $"l_orderkey" === atLeast1("key"))
      .groupBy($"l_orderkey")
      .agg(countDistinct($"l_suppkey").as("suppkey_count"))
      .filter($"suppkey_count" === 1)
      .select($"l_orderkey".as("key"))

    val forder = order.select($"o_orderkey", $"o_orderstatus")
      .filter($"o_orderstatus" === "F")

    val fsupplier = supplier.select($"s_suppkey", $"s_nationkey", $"s_name")
    val nat_supp = nation.filter($"n_name" === "SAUDI ARABIA")
      .join(fsupplier, $"n_nationkey" === fsupplier("s_nationkey"))

    val res = flineitem.join(exactly1, $"l_orderkey" === exactly1("key"))
      .join(nat_supp, $"l_suppkey" === nat_supp("s_suppkey"))
      .join(forder, $"l_orderkey" === forder("o_orderkey"))
      .groupBy($"s_name")
      .agg(count($"l_orderkey").as("numwait"))
      .sort($"numwait".desc, $"s_name")
      .limit(100)
    */

    res.collect().foreach(println)

  }

}