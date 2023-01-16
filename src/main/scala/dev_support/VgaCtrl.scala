package dev_support
import chisel3._
import chisel3.util._

class VgaCtrl extends Module {
  object Param {
    object H {
      val frontPorch = 96
      val active     = 144
      val backPorch  = 784
      val total      = 800
      val bitWid     = log2Ceil(total)
    }
    object V {
      val frontPorch = 2
      val active     = 35
      val backPorch  = 515
      val total      = 525
      val bitWid     = log2Ceil(total)
    }
    val colorVecWid = 8
    val width       = 640
    val height      = 480
  }
  val io = IO(new Bundle {
    val i = new Bundle {
      val clk     = Input(Clock())
      val rst     = Input(Reset())
      val vgaData = Input(UInt((Param.colorVecWid * 3).W))
    }
    val o = new Bundle {
      val hAddr = Output(UInt(Param.H.bitWid.W))
      val vAddr = Output(UInt(Param.V.bitWid.W))
      val hSync = Output(Bool())
      val vSync = Output(Bool())
      val valid = Output(Bool())
      val vgaR  = Output(UInt(Param.colorVecWid.W))
      val vgaG  = Output(UInt(Param.colorVecWid.W))
      val vgaB  = Output(UInt(Param.colorVecWid.W))
    }
  })
  withClockAndReset(io.i.clk, io.i.rst.asAsyncReset) {
    val xCnt = RegInit(1.U(Param.H.bitWid.W))
    val yCnt = RegInit(1.U(Param.V.bitWid.W))
    xCnt := Mux(xCnt === Param.H.total.U, 1.U, xCnt + 1.U)
    yCnt := Mux(
      xCnt === Param.H.total.U,
      Mux(
        yCnt === Param.V.total.U,
        1.U,
        yCnt + 1.U
      ),
      yCnt
    )

    io.o.hSync := xCnt > Param.H.frontPorch.U
    io.o.vSync := yCnt > Param.V.frontPorch.U

    val hValid = (xCnt > Param.H.active.U) && (xCnt <= Param.H.backPorch.U)
    val vValid = (yCnt > Param.V.active.U) && (yCnt <= Param.V.backPorch.U)
    io.o.valid := hValid && vValid

    io.o.hAddr := Mux(hValid, xCnt - (Param.H.active + 1).U, 0.U)
    io.o.vAddr := Mux(vValid, yCnt - (Param.V.active + 1).U, 0.U)

    io.o.vgaR := io.i.vgaData(Param.colorVecWid * 3 - 1, Param.colorVecWid * 2)
    io.o.vgaG := io.i.vgaData(Param.colorVecWid * 2 - 1, Param.colorVecWid * 1)
    io.o.vgaB := io.i.vgaData(Param.colorVecWid * 1 - 1, Param.colorVecWid * 0)
//    Cat(io.o.vgaR, io.o.vgaG, io.o.vgaB) := io.i.vgaData
  }
}
