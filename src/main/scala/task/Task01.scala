package task
import Chisel.{Cat, MuxLookup}
import chisel3._
import dev_support.NVBoardIOBundle

//  https://nju-projectn.github.io/dlco-lecture-note/exp/01.html
class Task01 extends Module {
  val io = IO(new NVBoardIOBundle)
  io.led := 0.U
  io.vga.getElements.foreach(x => x := 0.U)
  io.seg.foreach(x => x := 0.U)

  io.led := Cat(
    0.U,
    MuxLookup(
      io.sw(1, 0),
      default = 0.U(2.W),
      mapping = Array(0.U -> io.sw(3, 2), 1.U -> io.sw(5, 4), 2.U -> io.sw(7, 6), 3.U -> io.sw(9, 8)).toIndexedSeq
    )
  )
}
