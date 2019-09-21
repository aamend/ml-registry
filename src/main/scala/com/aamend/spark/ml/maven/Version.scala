package com.aamend.spark.ml.maven

import scala.util.{Failure, Success, Try}

/**
 * Version object to reference pipeline model that will be deployed
 * @param majorVersion The major version (using specific data), where version increment would lead to incompatibility
 * @param minorVersion The minor version (using specific configuration / parameters), where version increment should be backwards compatible
 * @param buildNumber The build number that will be incremented automatically any time a same model is deployed (anytime a same model is retrained)
 */
case class Version(
                    majorVersion: Int = 1,
                    minorVersion: Int = 0,
                    buildNumber: Option[Int] = None
                  ) extends Ordered[Version] {

  import scala.math.Ordered.orderingToOrdered

  override def compare(that: Version): Int = {
    (
      this.majorVersion,
      this.minorVersion,
      this.buildNumber
    ) compareTo (
      that.majorVersion,
      that.minorVersion,
      that.buildNumber
    )
  }

  override def toString: String = s"$majorVersion.$minorVersion.${buildNumber.getOrElse(0)}"

  /**
   * If build number is provided, we increment. If not, we define it as 0.
   * @return The exact version that will be deployed
   */
  def increment: Version = {
    buildNumber match {
      case Some(build) => Version(majorVersion, minorVersion, Some(build + 1))
      case None => Version(majorVersion, minorVersion, Some(0))
    }
  }
}

object Version {

  /**
   * Read a version object from string, where version is provided as [major].[minor].[build].
   * Build number is optional. No build number will result in previous build incremented.
   * Minor number is optional. No minor will result in version 0.
   * @param version the version provided as a string
   * @return the resulting version object
   */
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