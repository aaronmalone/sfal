package edu.osu.sfal.rest

import org.scalatest.FunSuite
import scala.util.parsing.json.JSONObject
import com.google.gson.{JsonElement, JsonParser, Gson}

class IncomingRequestTest extends FunSuite {
  test("Parse a properly-formatted JSON object to an incoming request.") {
    val json = """
      |{
      |  "model" : "cid",
      |  "timestep" : 7,
      |  "inputs" : {
      |    "startDate" : "branch01~subBranch02~startDate~0",
      |    "location" : "branch01~subBranch02~location~0",
      |    "temperature" : "branch01~subBranch02~temperature~7",
      |    "randomSeed" : "branch01~subBranch02~randomSeeForCid~7",
      |    "weekToSimulate" : "$timestep"
      |  },
      |  "outputs":{
      |    "demand" : "branch01~subBranch02~cidDemand~7"
      |   }
      |}
    """.stripMargin
    val jsonParser = new JsonParser()
    val element: JsonElement = jsonParser.parse(json)
    val ir: IncomingRequest = IncomingRequest.fromJson(element.getAsJsonObject)
    assert(ir != null)
    assert("cid" === ir.getSimulationFunctionName.toString)
    assert(7 === ir.getTimestep)
    for(input <- Array("startDate", "location", "temperature", "randomSeed", "weekToSimulate")) {
      assert(ir.getInputs.containsKey(input), s"Inputs map does not contain $input")
    }
    assert("branch01~subBranch02~temperature~7" === ir.getInputs.get("temperature"))
    assert(ir.getOutputs.containsKey("demand"))
    assert("branch01~subBranch02~cidDemand~7" === ir.getOutputs.get("demand"))
  }
}
