package dmos.svarus

import javax.servlet.http.{
  HttpServlet,
  HttpServletRequest => JReq,
  HttpServletResponse => JResp}

import scala.language.implicitConversions

object GET extends Method("GET")
object PUT extends Method("PUT")
object POST extends Method("POST")
object DELETE extends Method("DELETE")

case class Path(els:List[String])

object Path {
  def unapply(req:Req):Option[List[String]] = req.path match {
    case Path("" :: rest) => Some(rest)
    case Path(whole) => Some(whole)
  }
}

case class Req(jreq:JReq) {
  val method = jreq.getMethod
  val path = Path(jreq.getRequestURI.split("/").toList)
  lazy val body = {
    val reader = jreq.getReader
    val string =
      Stream
        .continually(reader.readLine())
        .takeWhile(_ != null)
        .mkString("\n")
    reader.close
    string
  }
}

class Resp(val status:Int, val body:String)

object Resp {
  def status(i:Int) = new Resp(status=i, body="")
  lazy val ok = status(200)
  lazy val created = status(201)
  lazy val bad = status(400)
  lazy val notFound = status(404)
  lazy val error = status(500)
}

trait Servlet extends HttpServlet {

  def serve(req:Req):Resp

  override def service(jreq:JReq, jresp:JResp) = {
    val resp = serve(Req(jreq))
    val writer = jresp.getWriter
    writer.write(resp.body)
    jresp.setStatus(resp.status)
    writer.close
  }
}

abstract class Method(method:String) {
  def unapply(req:Req) = Some(req).filter(_.method == method)
}

import dmos.gae.Datastore

class App extends Servlet {

  private implicit def fs2resp(fs:FilmuSarasas):Resp =
    new Resp(200, fs.sarasas)

  def gaukFilmuSarasa:FilmuSarasas =
    try {
      Datastore.gauk[FilmuSarasas](FilmuSarasas.key)
    } catch {
      case e: Datastore.NotFound =>
        FilmuSarasas.empty
    }

  def isNumber(s:String) = s.forall(Character.isDigit(_))

  def serve(req:Req):Resp = {
    req match {
      case GET(Path("filmai" :: sub)) => sub match {
        case Nil => gaukFilmuSarasa
        case "atgal" :: perKiek :: Nil => {
          if (isNumber(perKiek))
            try {
              gaukFilmuSarasa.atgalPer(perKiek.toInt)
            } catch {
              case e:NoSuchVersion => Resp.bad
            }
          else
            Resp.bad
        }
        case _ => Resp.notFound
      }
      case PUT(Path("filmai" :: Nil)) => 
        Datastore.issaugok[FilmuSarasas](gaukFilmuSarasa.atnaujink(req.body))
      case _ => Resp.notFound
    }
  }
}
