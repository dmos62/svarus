package dmos.svarus

import javax.servlet.http.{
  HttpServlet,
  HttpServletRequest => JReq,
HttpServletResponse => JResp}

import scala.language.implicitConversions

abstract class Method(method:String) {
  def unapply(req:Req) = Some(req).filter(_.method == method)
}

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

object ContextPath {
  def unapply(req:Req):Option[List[String]] = req.contextPath match {
    case Path("" :: rest) => Some(rest)
    case Path(whole) => Some(whole)
  }
}

case class Req(jreq:JReq) {
  val method = jreq.getMethod
  private def string2Path(s:String) = Path(s.split("/").toList)
  lazy val servletPath = string2Path(jreq.getServletPath)
  lazy val path = string2Path(jreq.getRequestURI)
  lazy val contextPath = Path(path.els.drop(servletPath.els.length))
  lazy val rootUrl = 
    jreq.getScheme + "://" + jreq.getServerName + ":" + jreq.getServerPort
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

class Resp(val status:Int, val body:String) {
  def body(s:String):Resp = new Resp(status=status, body=s)

  def body(html:HtmlDoc):Resp = this.body(html.toString)
}

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

case class HtmlDoc( 
  title:String,
  jses:List[String],
  csses:List[String],
  body:xml.NodeSeq) {

  def title(s:String):HtmlDoc = this.copy(title = s)
  def js(path:String) = this.copy(jses = this.jses :+ path)
  def css(path:String) = this.copy(csses = this.csses :+ path)
  def body(html:xml.NodeSeq):HtmlDoc = this.copy(body = html)

  lazy override val toString:String = 
    {
      <html>
        <head>
          <meta charset="utf-8"/>
          <title>{ title }</title>
          { for { path <- jses } yield
            <script src={ path }></script> }
          { for { path <- csses } yield
            <link href={ path } rel="stylesheet"></link> }
        </head>
        <body>{ body }</body>
      </html>
    }.toString
}

object HtmlDoc
  extends HtmlDoc(
    title="",
    jses=Nil,
    csses=Nil,
    body=Nil)

class Filmai extends Servlet {

  import dmos.gae.Datastore

  implicit def fs2resp(fs:FilmuSarasas):Resp =
    Resp.ok.body(fs.sarasas)

  def gaukFilmuSarasa:FilmuSarasas =
    try {
      Datastore.gauk[FilmuSarasas](FilmuSarasas.key)
    } catch {
      case e: Datastore.NotFound => FilmuSarasas.empty
    }

  def isNumber(s:String) = s.forall(Character.isDigit(_))

  def serve(req:Req):Resp = {
    req match {
      case GET(ContextPath(sub)) => sub match {
        case Nil => gaukFilmuSarasa
        case "atgal" :: perKiek :: Nil => {
          if (isNumber(perKiek))
            try {
              gaukFilmuSarasa.atgalPer(perKiek.toInt)
            } catch {
              case _:NoSuchVersion => Resp.bad
            }
          else
            Resp.bad
        }
        case "kiek" :: Nil =>
          Resp.ok.body(gaukFilmuSarasa.versijos.length.toString)
        case _ => Resp.notFound
      }
      case PUT(ContextPath(Nil)) => 
        Datastore.issaugok[FilmuSarasas](gaukFilmuSarasa.atnaujink(req.body))
      case _ => Resp.notFound
    }
  }
}

class ForHumans extends Servlet {

  lazy val jsPage =
    HtmlDoc
      .title("Laba diena")
      .js("/js/main.js")
      .css("/css/main.css")
      .body(<div id="container"></div>)

  def serve(req:Req) = Resp.ok.body(jsPage)
}

class Public extends Servlet {

  import dmos.gae.Users

  def serve(req:Req) = {
    req match {
      case GET(_) => req match {
        case ContextPath("filmai" :: Nil) => {
          val filmai = new Filmai
          import filmai.fs2resp
          filmai.gaukFilmuSarasa
        }
        case Path("admin_q" :: Nil) =>
          if (Users.isAdmin) Resp.ok.body("yes")
          else Resp.ok.body("no")
        case Path("login" :: Nil) => Resp.ok.body(Users.loginUrl(req.rootUrl))
        case _ => Resp.notFound
      }
      case _ => Resp.bad
    }
  }
}
