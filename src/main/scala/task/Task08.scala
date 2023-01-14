package task
import chisel3._
import dev_support.NVBoardIOBundle

class Task08 extends Module {
  val io = IO(new NVBoardIOBundle)
  io.led := 0.U
  io.seg.foreach(x => x := 0.U(8.W))
  io.vga.getElements.foreach(x => x := 0.U(8.W))

}
