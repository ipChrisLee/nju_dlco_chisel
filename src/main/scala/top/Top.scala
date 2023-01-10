package top
import chisel3._
import chisel3.util.{Cat, MuxLookup}
import dev_support.NVBoardIOBundle

class Top extends Module {
  val io = IO(new NVBoardIOBundle {
    val value1        = Input(UInt(16.W))
    val value2        = Input(UInt(16.W))
    val loadingValues = Input(Bool())
    val outputGCD     = Output(UInt(16.W))
    val outputValid   = Output(Bool())
  })
  io.led := 0.U
  io.vga.getElements.foreach(x => x := 0.U)
  io.seg.foreach(x => x := 0.U)

  val x = Reg(UInt())
  val y = Reg(UInt())

  when(x > y) {
    x := x - y
  }.otherwise {
    y := y - x
  }

  when(io.loadingValues) {
    x := io.value1
    y := io.value2
  }

  io.outputGCD   := 0.U
  io.outputGCD   := x
  io.outputValid := y === 0.U

  io.led := Cat(
    0.U,
    MuxLookup(
      io.sw(1, 0),
      default = 0.U(2.W),
      mapping = Array(0.U -> io.sw(3, 2), 1.U -> io.sw(5, 4), 2.U -> io.sw(7, 6), 3.U -> io.sw(9, 8)).toIndexedSeq
    )
  )
}
