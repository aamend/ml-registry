package com.aamend.spark.gov.nexus

import scala.util.{Failure, Success, Try}

case class Version(
                    majorVersion: Int = 1,
                    minorVersion: Int = 0,
                    buildNumber: Option[Int] = None
                  ) extends Ordered[Version] {

  import scala.math.Ordered.orderingToOrdered

  override def compare(that: Version): Int = {
    (this.majorVersion, this.minorVersion, this.buildNumber) compareTo (that.majorVersion, that.minorVersion, that.buildNumber)
  }

  override def toString: String = s"$majorVersion.$minorVersion.${buildNumber.getOrElse(0)}"

  def increment: Version = {
    if (buildNumber.isDefined) {
      Version(majorVersion, minorVersion, Some(buildNumber.get + 1))
    } else {
      Version(majorVersion, minorVersion, Some(0))
    }
  }

}

object Version {

  def apply(version: String): Version = {

    Try {
      val v = version.split("\\.")
      v.length match {
        case 0 => throw new IllegalArgumentException("At least major version must be specified")
        case 1 => new Version(v(0).toInt)
        case 2 => new Version(v(0).toInt, v(1).toInt)
        case _ => new Version(v(0).toInt, v(1).toInt, Some(v(2).toInt))
      }
    } match {
      case Success(v) => v
      case Failure(e) =>
        throw new IllegalArgumentException("Version must follow [major].[minor].[build] pattern", e)
    }

  }

  def isExplicit(version: String): Boolean = "^\\d+\\.\\d+\\.\\d+$".r.findFirstMatchIn(version).isDefined

}