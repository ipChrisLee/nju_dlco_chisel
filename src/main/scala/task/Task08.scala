package task
import chisel3._
import chisel3.util._
import dev_support.{ClockDivider, NVBoardIOBundle, VgaCtrl}
import os.FilePath

class Task08 extends Module {
  val io = IO(new NVBoardIOBundle)
  io.led := 0.U
  io.seg.foreach(x => x := ~0.U(8.W))

//  val vgaClk  = ClockDivider(clockIn = clock, divideBy = 4)
  val vgaClk  = clock
  val vgaCtrl = Module(new VgaCtrl)
  vgaCtrl.io.i.clk := vgaClk
  vgaCtrl.io.i.rst := reset
  io.vga.clk       := vgaClk.asBool
  io.vga.blankN    := vgaCtrl.io.o.valid
  io.vga.vSync     := vgaCtrl.io.o.vSync
  io.vga.hSync     := vgaCtrl.io.o.hSync
  io.vga.r         := vgaCtrl.io.o.vgaR
  io.vga.g         := vgaCtrl.io.o.vgaG
  io.vga.b         := vgaCtrl.io.o.vgaB

  val picMemoryFilePath = "resource/picture.txt"
  val mem               = Mem(524288, UInt((vgaCtrl.Param.colorVecWid * 3).W))
  chisel3.util.experimental.loadMemoryFromFileInline(memory = mem, fileName = picMemoryFilePath)
  val memIdx = Cat(vgaCtrl.io.o.hAddr(9, 0), vgaCtrl.io.o.vAddr(8, 0))
  assert(memIdx < mem.length.U)
  vgaCtrl.io.i.vgaData := mem(memIdx)
}
