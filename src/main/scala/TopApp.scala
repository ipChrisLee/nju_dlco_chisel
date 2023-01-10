import top.Top

object TopApp extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Top, args)
}
