package fr.xebia.streams.common

import scala.language.reflectiveCalls

object IoOps {

  def using[A, B <: { def close(): Unit }](closeable: B)(f: B => A): A =
    try { f(closeable) } finally { closeable.close() }

  def using2[A, B <: { def close(): Unit }, C <: { def close(): Unit }](closeB: B, closeC: C)(f: (B, C) => A): A = {
    try { f(closeB, closeC) } finally { closeB.close(); closeC.close() }
  }

}
