package edu.osu.sfal.rest

import org.scalatest.FunSuite
import edu.osu.sfal.data.SfalDaoInMemoryImpl
import org.restlet.Request
import org.restlet.data.Method
import java.util
import com.google.gson.JsonParser

class DataRestletTest extends FunSuite {

  val sfalDao = new SfalDaoInMemoryImpl()
  val expectedLookupValue = util.Arrays.asList(1.1, 2.2, 3.3)
  sfalDao.save("keyZero", 0)
  sfalDao.save("keyOne", expectedLookupValue)
  sfalDao.save("keyTwo", "hello world")

  val restlet = new DataRestlet(sfalDao)

  val parser = new JsonParser()

  test("Test retrieval of one key") {
    val request = new Request(Method.POST, "resource/cache/keyOne")
    request.getAttributes.put("dataStoreKey", "keyOne")
    val response = restlet.handle(request)
    assert(200 === response.getStatus.getCode)
    val jsonArray = parser.parse(response.getEntityAsText).getAsJsonArray
    assert(3 === jsonArray.size)
    assert(1.1 == jsonArray.get(0).getAsDouble)
    assert(2.2 == jsonArray.get(1).getAsDouble)
    assert(3.3 == jsonArray.get(2).getAsDouble)
  }

  test("Test retrieval of all keys") {
    val request = new Request(Method.POST, "resource/cache")
    val response = restlet.handle(request)
    assert(200 === response.getStatus.getCode)
    val jsonObj = parser.parse(response.getEntityAsText).getAsJsonObject
    assert(0 === jsonObj.getAsJsonPrimitive("keyZero").getAsInt)
    assert("hello world" === jsonObj.getAsJsonPrimitive("keyTwo").getAsString)
    assert(3 == jsonObj.getAsJsonArray("keyOne").size)
  }
}
