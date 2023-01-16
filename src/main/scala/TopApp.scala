import top.Top
import dev_support.VgaCtrl
import chisel3._

object TopApp extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Top, args)
}
