package com.aamend.spark.ml.maven

import org.scalatest.{FlatSpec, Matchers}

class VersionTest extends FlatSpec with Matchers {

  val valid = "1.1.1"
  val major = "1"
  val majorMinor = "1.1"
  val invalid = "1.1.1xxx"

  "Version" should "be retrieved from string" in {
    Version.isExplicit(valid) should be(true)
    val v = Version(valid)
    v.majorVersion should be(1)
    v.minorVersion should be(1)
    v.buildNumber should be(Some(1))
  }

  it should "be incremented" in {
    val v = Version(valid).increment
    v.buildNumber should be(Some(2))
  }

  "An invalid version" should "throw exception" in {
    intercept[IllegalArgumentException] {
      Version(invalid)
    }
    intercept[IllegalArgumentException] {
      Version("")
    }
  }

  "Version without build" should "be retrieved from string" in {
    Version.isExplicit(majorMinor) should be(false)
    val v = Version(majorMinor)
    v.majorVersion should be(1)
    v.minorVersion should be(1)
    v.buildNumber should be(None)
  }

  "Version without minor and build" should "be retrieved from string" in {
    Version.isExplicit(major) should be(false)
    val v = Version(major)
    v.majorVersion should be(1)
    v.minorVersion should be(0)
    v.buildNumber should be(None)
  }

}
