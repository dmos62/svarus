package dmos.svarus

import scala.language.implicitConversions

import dmos.gae
import gae.{Datastore, Blobstore, Objektas}
import Datastore.{Entity, Kind}
import Blobstore.BlobKey

import play.api.libs.json.{Json => PlayJson, Writes, Reads, Format}

object Kindai {
  val FilmuSarasas = Kind("filmusarasas")
}

import Patcher.Patch

case class FilmuSarasas(
  sarasas:String,
  versijos:List[Versija]
) extends Objektas {
  val kind = Kindai.FilmuSarasas
  val blobs = Set.empty[BlobKey]

  val name = "filmusarasas"
  val key = Datastore.nameToKey(kind, name)
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

case class Versija(laikas:java.util.Date, patch:Patch)

object Versija {
  implicit val format:Format[Versija] = PlayJson.format[Versija]
}

object Patcher {
  import name.fraser.neil.plaintext.diff_match_patch
  import scala.collection.JavaConverters._

  private type SubPatch = diff_match_patch.Patch

  private lazy val dmp = new diff_match_patch

  class Patch(val subPatches:java.util.LinkedList[SubPatch])

  object Patch {
    implicit val writes:Writes[Patch] =
      Writes[Patch] { p:Patch =>
        Writes.of[String].writes(serialize(p.subPatches))
      }

    implicit val reads:Reads[Patch] =
      Reads.of[String].map(deserialize(_))

  }

  private implicit def subPatches2patch(
    subPatches:java.util.LinkedList[SubPatch]):Patch =
      new Patch(subPatches)

  private implicit def patch2subPatches(
    p:Patch):java.util.LinkedList[SubPatch] =
      p.subPatches

  def make(from:String, to:String):Patch = dmp.patch_make(from, to)

  def apply(patch:Patch, to:String):String =
    dmp.patch_apply(patch, to)(0).asInstanceOf[String]

  def serialize(patch:Patch):String = dmp.patch_toText(patch)

  def deserialize(serialized:String):Patch = dmp.patch_fromText(serialized)

}
