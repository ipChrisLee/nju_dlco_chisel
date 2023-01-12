package dev_support
import chisel3._

class NVBoardIOBundle extends Bundle {
  val sw = Input(UInt(16.W))
  val ps2 = new Bundle {
    val clk  = Input(Bool())
    val data = Input(Bool())
  }
  val btn = new Bundle {
    val c = Input(Bool())
    val u = Input(Bool())
    val d = Input(Bool())
    val l = Input(Bool())
    val r = Input(Bool())
  }
  val led = Output(UInt(16.W))
  val vga = new Bundle {
    val clk    = Output(Bool())
    val hSync  = Output(Bool())
    val vSync  = Output(Bool())
    val blankN = Output(Bool())
    val r      = Output(UInt(8.W))
    val g      = Output(UInt(8.W))
    val b      = Output(UInt(8.W))
  }
  val seg = Vec(8, Output(UInt(8.W)))
}
/*
io.led := 0.U
io.vga.getElements.foreach(x => x := 0.U)
io.seg.foreach(x => x := 0.U)
 */
