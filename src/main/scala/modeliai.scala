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

case class FilmuSarasas(
  sarasas:String,
  versijos:Stack[Versija]
) extends Objektas {
  val kind = Kindai.FilmuSarasas
  val name = "filmusarasas"
  lazy val key = Datastore.nameToKey(kind, name)
  lazy val blobs = Set.empty[BlobKey]

  def atnaujink(naujasSarasas:String):FilmuSarasas = {
    val naujaVersija = Versija(Date(), Patcher.make(sarasas, naujasSarasas))
    FilmuSarasas(naujasSarasas, versijos.push(naujaVersija))
  }

  def atgal = atgalPer(1)

  def atgalPer(kiek:Int) = {
    if (kiek > versijos.length) throw new java.lang.IndexOutOfBoundsException
    val patch = versijos.take(kiek).map(_.patch).reduceLeft( _ join _ )
    FilmuSarasas(Patcher.apply(patch, sarasas), versijos.drop(kiek))
  }
}

object FilmuSarasas {
  import gae.{EntityFrom, ObjektasFrom}

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
