package dmos.svarus

import scala.language.implicitConversions

import play.api.libs.json.{Json => PlayJson, Writes, Reads, Format}

object Patcher {
  import name.fraser.neil.plaintext.diff_match_patch
  import scala.collection.JavaConverters._

  private type SubPatch = diff_match_patch.Patch

  private lazy val dmp = new diff_match_patch

  import java.util.LinkedList

  class Patch(val subPatches:LinkedList[SubPatch]) {
    def join(patch:Patch) = {
      val newSubPatches = new LinkedList(patch.subPatches)
      newSubPatches addAll subPatches
      new Patch(newSubPatches)
    }
  }

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
