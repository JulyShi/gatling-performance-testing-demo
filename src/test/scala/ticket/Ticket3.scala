package ticket

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class Ticket3 extends Simulation {

  val httpConf = http.baseURL( "http://127.0.0.1:8888" )

  object ticketCode {

    val ticketCode = exec(
      http( "get ticket code" )
        .post( "/code/myd1" )
        .header( "Content-Type", "application/x-www-form-urlencoded" )
        .formParam("idnum", "61032119000203132X")
        .formParam("idtype", "2")
        .check( status.is( 200 ) )
        .check( jsonPath( "$.account" ).find.saveAs( "account" ) )
        .check( jsonPath( "$.password" ).find.saveAs( "password" ) ) )
  }

  object login {
    val random = new scala.util.Random

    // Generate a random string of length n from the given alphabet
    def randomString(alphabet: String)(n: Int): String =
      Stream.continually( random.nextInt( alphabet.size ) ).map( alphabet ).take( n ).mkString

    // Generate a random alphabnumeric string of length n
    def randomAlphanumericString(n: Int) =
      randomString( "abcdef0123456789" )( n )

    def mac(): String = {
      randomAlphanumericString( 12 ).grouped( 2 ).toList.mkString( ":" )
    }

    //val random = new util.Random
    //val randomMac = Iterator.continually(Array.fill[String](6)(f"${random.nextInt(255)}%02x").mkString(":"))

    val body = StringBody(
      """{
        |"username": "${account}",
        |"password": "${password}",
        |"mac": "${mac}"
        |}""".stripMargin )

    val login = exec( session => {
      session.set( "mac", mac() )
    } ).pause( 2 ).exec(
      http( "login with ticket" )
        .post( "/auth" )
        .header( "Content-Type", "application/json" )
        .body( body ).asJSON
        .check( status.is( 200 ) ) )
  }

  val scn = scenario( "ticket" )
    exec( ticketCode.ticketCode, login.login )



  //一次并发每秒580用户
  setUp(
    scn.inject( atOnceUsers( 580 ) )
  ).protocols( httpConf )

}
