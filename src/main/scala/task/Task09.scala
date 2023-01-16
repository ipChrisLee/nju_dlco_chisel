package task
import chisel3._
import chisel3.util._
import dev_support._
import dev_support.PS2._

object WInfo {
  object Screen {
    val height = 480
    val width  = 640
  }
  object Square {
    val height = 16
    val width  = 9
  }
  object Cursor {
    val rowCnt = 30
    val colCnt = 70
  }
  val wForCh                 = 7
  val wForBias               = 4
  val asciiFontTableFilePath = "resource/vga_font.txt"
}
class VgaPart extends Module {
  //  clock is system clock
  val io = IO(new Bundle {
    val vga = new Bundle {
      val clk    = Output(Bool())
      val hSync  = Output(Bool())
      val vSync  = Output(Bool())
      val blankN = Output(Bool())
      val r      = Output(UInt(8.W))
      val g      = Output(UInt(8.W))
      val b      = Output(UInt(8.W))
    }
    val cursor = new Bundle {
      val row = Input(UInt(WInfo.wForCh.W))
      val col = Input(UInt(WInfo.wForCh.W))
    }
    val dispMat = new Bundle {
      val iRow    = Output(UInt(WInfo.wForCh.W))
      val iCol    = Output(UInt(WInfo.wForCh.W))
      val biasRow = Output(UInt(WInfo.wForBias.W))
      val biasCol = Output(UInt(WInfo.wForBias.W))
      val isWhite = Input(Bool())
    }
  })
  val vgaCtrl = Module(new VgaCtrl)
  vgaCtrl.io.i.clk := clock
  vgaCtrl.io.i.rst := reset
  io.vga.clk       := clock.asBool
  io.vga.blankN    := vgaCtrl.io.o.valid
  io.vga.vSync     := vgaCtrl.io.o.vSync
  io.vga.hSync     := vgaCtrl.io.o.hSync
  io.vga.r         := vgaCtrl.io.o.vgaR
  io.vga.g         := vgaCtrl.io.o.vgaG
  io.vga.b         := vgaCtrl.io.o.vgaB

  //  pixel position
  val vAddr = vgaCtrl.io.o.vAddr
  val hAddr = vgaCtrl.io.o.hAddr
  val row   = vAddr / 16.U(10.W)
  val col   = hAddr / 9.U(10.W)
  val rBias = vAddr % 16.U
  val cBias = hAddr % 9.U

  io.dispMat.iRow    := row
  io.dispMat.iCol    := col
  io.dispMat.biasRow := rBias
  io.dispMat.biasCol := cBias

  //  counter for cursor
  val cursorBlinkFrameCnt = 3
  val cursorCounter =
    withClockAndReset(vgaCtrl.io.o.vSync.asClock, reset.asAsyncReset) {
      val c = Counter(cursorBlinkFrameCnt * 2)
      c.inc()
      c
    }
  val cursorLight = io.cursor.col === col && io.cursor.row === row && cursorCounter.value < cursorBlinkFrameCnt.U

  //  generate pixel
  val pixel = io.dispMat.isWhite ^ cursorLight
  vgaCtrl.io.i.vgaData := Fill(24, pixel)
}

class KeyboardPart extends Module {
  //  clock is ps2.clk
  val io = IO(new Bundle {
    val ps2 = new Bundle {
      val dat = Input(Bool())
    }
    val cursor = new Bundle {
      val row = Output(UInt(WInfo.wForCh.W))
      val col = Output(UInt(WInfo.wForCh.W))
    }
    val dispMat = new Bundle {
      val iRow    = Input(UInt(WInfo.wForCh.W))
      val iCol    = Input(UInt(WInfo.wForCh.W))
      val biasRow = Input(UInt(WInfo.wForBias.W))
      val biasCol = Input(UInt(WInfo.wForBias.W))
      val isWhite = Output(Bool())
    }
  })
  //  transmitter and lut
  val keyboardTransmitter = Module(new Transmitter)
  keyboardTransmitter.io.ps2.clk := clock.asBool
  keyboardTransmitter.io.ps2.dat := io.ps2.dat
  val ctrlClk  = keyboardTransmitter.io.valid.asClock
  val scanCode = keyboardTransmitter.io.scanCode
  val isBreak  = keyboardTransmitter.io.isBreak

  val keyboardLUT = Module(new ScanCodeToAsciiBiPortLUT)
  keyboardLUT.io.scanCode := keyboardTransmitter.io.scanCode
  val isAscii        = keyboardLUT.io.isAscii
  val asciiCode      = keyboardLUT.io.asciiCode
  val upperAsciiCode = keyboardLUT.io.upperAsciiCode
  val isLetter       = keyboardLUT.io.isLetter

  //  display mat mem
  val dispMem =
    withClockAndReset(ctrlClk, reset.asAsyncReset) {
      val dispMem      = RegInit(VecInit.fill(WInfo.Cursor.rowCnt * WInfo.Cursor.colCnt)(0.U(8.W)))
      val asciiFontMem = Mem(256 * WInfo.Square.height, UInt(WInfo.Square.width.W))
      chisel3.util.experimental.loadMemoryFromFileInline(asciiFontMem, WInfo.asciiFontTableFilePath)
      val idxMem = io.dispMat.iRow * WInfo.Cursor.colCnt.U + io.dispMat.iCol
      io.dispMat.isWhite := Mux(
        io.dispMat.iCol < WInfo.Cursor.colCnt.U,
        asciiFontMem(dispMem(idxMem) * WInfo.Square.height.U + io.dispMat.biasRow)(io.dispMat.biasCol),
        false.B
      )
      dispMem
    }

  //  keyboard status
  val (capsOn, shiftOn) =
    withClockAndReset(ctrlClk, reset.asAsyncReset) {
      val capsOn = RegInit(false.B)
      capsOn := Mux(isBreak && scanCode === ScanCodeInfo.caps.U, ~capsOn, capsOn)
      val shiftOn = RegInit(false.B)
      shiftOn := Mux(
        scanCode === ScanCodeInfo.lShift.U || scanCode === ScanCodeInfo.rShift.U,
        Mux(isBreak, false.B, true.B),
        shiftOn
      )
      (capsOn, shiftOn)
    }

  //  cursor reg
  val (cursorRow, cursorCol) =
    withClockAndReset(ctrlClk, reset.asAsyncReset) {
      val cursorRow = RegInit(0.U(WInfo.wForCh.W))
      val cursorCol = RegInit(0.U(WInfo.wForCh.W))
      io.cursor.row := cursorRow
      io.cursor.col := cursorCol
      val cursorNxtRow = Mux(
        cursorCol === (WInfo.Cursor.colCnt - 1).U, // last col
        Mux(
          cursorRow === (WInfo.Cursor.rowCnt - 1).U,
          0.U,
          cursorRow + 1.U
        ),
        cursorRow
      )
      val cursorNxtCol = Mux(
        cursorCol === (WInfo.Cursor.colCnt - 1).U,
        0.U,
        cursorCol + 1.U
      )
      val cursorPreRow = Mux(
        cursorCol === 0.U,
        Mux(
          cursorRow === 0.U,
          (WInfo.Cursor.rowCnt - 1).U,
          cursorRow - 1.U
        ),
        cursorRow
      )
      val cursorPreCol = Mux(
        cursorCol === 0.U,
        (WInfo.Cursor.colCnt - 1).U,
        cursorCol - 1.U
      )

      when(!isBreak) {
        when(isAscii) {
          cursorRow := cursorNxtRow
          cursorCol := cursorNxtCol
        }.elsewhen(scanCode === ScanCodeInfo.upArr.U) {
          cursorRow := Mux(cursorRow === 0.U, (WInfo.Cursor.rowCnt - 1).U, cursorRow - 1.U)
          cursorCol := cursorCol
        }.elsewhen(scanCode === ScanCodeInfo.leftArr.U) {
          cursorRow := cursorPreRow
          cursorCol := cursorPreCol
        }.elsewhen(scanCode === ScanCodeInfo.rightArr.U) {
          cursorRow := cursorNxtRow
          cursorCol := cursorNxtCol
        }.elsewhen(scanCode === ScanCodeInfo.downArr.U) {
          cursorRow := Mux(cursorRow === (WInfo.Cursor.rowCnt - 1).U, 0.U, cursorRow + 1.U)
          cursorCol := cursorCol
        }.elsewhen(scanCode === ScanCodeInfo.enter.U) {
          cursorRow := Mux(cursorRow === (WInfo.Cursor.rowCnt - 1).U, 0.U, cursorRow + 1.U)
          cursorCol := cursorCol
        }
      }
      //  TODO
      (cursorRow, cursorCol)
    }
  val cursorIdx = cursorRow * WInfo.Cursor.colCnt.U + cursorCol

  //  input
  withClockAndReset(ctrlClk, reset.asAsyncReset) {
    when(!isBreak) {
      when(isAscii) {
        dispMem(cursorIdx) := Mux(
          isLetter,
          Mux(capsOn || shiftOn, upperAsciiCode, asciiCode),
          Mux(shiftOn, upperAsciiCode, asciiCode)
        )
      }.elsewhen(scanCode === ScanCodeInfo.backSpace.U) {
        dispMem(cursorIdx) := 0.U(8.W)
      }
    }
  }

}

class Task09 extends Module {
  val io = IO(new NVBoardIOBundle)
  io.led := 0.U
  io.seg.foreach(x => x := 0xff.U)

  val vgaPart = Module(new VgaPart)
  val keyboardPart = withClockAndReset(io.ps2.clk.asClock, reset.asAsyncReset) {
    Module(new KeyboardPart)
  }

  vgaPart.io.cursor.row      := keyboardPart.io.cursor.row
  vgaPart.io.cursor.col      := keyboardPart.io.cursor.col
  vgaPart.io.dispMat.isWhite := keyboardPart.io.dispMat.isWhite

  keyboardPart.io.ps2.dat         := io.ps2.data
  keyboardPart.io.dispMat.iRow    := vgaPart.io.dispMat.iRow
  keyboardPart.io.dispMat.iCol    := vgaPart.io.dispMat.iCol
  keyboardPart.io.dispMat.biasRow := vgaPart.io.dispMat.biasRow
  keyboardPart.io.dispMat.biasCol := vgaPart.io.dispMat.biasCol

  io.vga.clk    := vgaPart.io.vga.clk
  io.vga.blankN := vgaPart.io.vga.blankN
  io.vga.vSync  := vgaPart.io.vga.vSync
  io.vga.hSync  := vgaPart.io.vga.hSync
  io.vga.r      := vgaPart.io.vga.r
  io.vga.g      := vgaPart.io.vga.g
  io.vga.b      := vgaPart.io.vga.b
}
