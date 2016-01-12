package dmos.svarus

import scala.language.implicitConversions

import dmos.gae
import gae.{Datastore, Blobstore, Objektas}
import Datastore.{Entity, Kind}
import Blobstore.BlobKey

import play.api.libs.json.{Json => PlayJson, Writes, Reads, Format}

import java.util.Date

import Patcher.Patch

object Kindai {
  val FilmuSarasas = Kind("filmusarasas")
}

trait BadCall extends Exception

class NoSuchVersion extends BadCall

case class FilmuSarasas(
  sarasas:String,
  versijos:List[Versija]
) extends Objektas {
  val kind = FilmuSarasas.kind
  lazy val key = FilmuSarasas.key
  lazy val blobs = Set.empty[BlobKey]

  def atnaujink(naujasSarasas:String):FilmuSarasas = {
    val naujaVersija = Versija(new Date, Patcher.make(naujasSarasas, sarasas))
    FilmuSarasas(naujasSarasas, naujaVersija :: versijos)
  }

  def atgal = atgalPer(1)

  def atgalPer(kiek:Int) = {
    if (1 > kiek || kiek > versijos.length) throw new NoSuchVersion
    val patch = versijos.take(kiek).map(_.patch).reduceLeft( _ join _ )
    FilmuSarasas(Patcher.apply(patch, sarasas), versijos.drop(kiek))
  }
}

object FilmuSarasas {
  import gae.{EntityFrom, ObjektasFrom}

  val kind = Kindai.FilmuSarasas
  lazy val key = Datastore.nameToKey(kind, kind.name)

  lazy val empty = FilmuSarasas("", List.empty)

  implicit val objektasFrom = ObjektasFrom[FilmuSarasas] { ent =>
    new FilmuSarasas(
      ent.getUnindexedString('sarasas),
      PlayJson.parse(ent.getUnindexedString('versijos)).as[List[Versija]])
  }

  implicit val entityFrom = EntityFrom[FilmuSarasas] { fs =>
    Entity(fs.key)
      .setUnindexedString('sarasas, fs.sarasas)
      .setUnindexedString('versijos,
        PlayJson.stringify(PlayJson.toJson(fs.versijos)))
  }
  
}

case class Versija(laikas:Date, patch:Patch)

object Versija {
  implicit val format:Format[Versija] = PlayJson.format[Versija]
}
