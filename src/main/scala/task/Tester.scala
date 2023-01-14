package task
import chisel3._
import chisel3.util._
import dev_support._

class Tester extends Module {
  val io = IO(new NVBoardIOBundle)
  io.led := 0.U
  io.vga.getElements.foreach(x => x := 0.U)
  io.seg.foreach(x => x := ~0.U(8.W))

  io.led := Cat(0.U, ClockDivider(clock, 4).asBool)
}
