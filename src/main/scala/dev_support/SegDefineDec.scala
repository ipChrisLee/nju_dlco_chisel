package dev_support
import chisel3._
import chisel3.util.{Cat, MuxLookup}
class SegDefineDec extends Module {
  val io = IO {
    new Bundle {
      val num    = Input(UInt(4.W))
      val hasDot = Input(Bool())
      val seg    = Output(UInt(8.W))
    }
  }
  io.seg := Cat(
    ~io.hasDot,
    MuxLookup(
      io.num,
      ~"b0".U(7.W),
      mapping = Array(
        0.U -> "b1000000",
        1.U -> "b1111001",
        2.U -> "b0100100",
        3.U -> "b0110000",
        4.U -> "b0011001",
        5.U -> "b0010010",
        6.U -> "b0000010",
        7.U -> "b1111000",
        8.U -> "b0000000",
        9.U -> "b0010000"
      ).collect[(UInt, UInt)] { case s: (UInt, String) => (s._1, s._2.U(7.W)) }.toIndexedSeq
    )
  )
}
