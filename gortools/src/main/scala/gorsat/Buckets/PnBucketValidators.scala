package gorsat.Buckets

object PnBucketValidators {
  def passThrough(pn: String): Boolean = {
    true
  }

  def deleted(pn: String): Boolean = {
    !pn.startsWith("#deleted#")
  }
}
