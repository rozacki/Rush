import java.security.MessageDigest
/*
knoal has a source
knol is immutable
know can exist in many places in the same time
 */
class Knol(data:String){
  val Hash:Array[Byte]      =   MessageDigest.getInstance("MD5").digest(data.getBytes)
  val Source:String =   null
  val Data:String   =   data
}
