#include "init.hpp"

#include <verilated.h>
#include <verilated_vcd_c.h>

#include <VTop.h>

#include <iostream>
#include <memory>
#include <filesystem>

#ifdef HAS_NVBOARD
#include "nvboard_include/nvboard.h"
void nvboard_bind_all_pins(VTop *);
#endif

std::unique_ptr<VerilatedContext> pContext;
std::unique_ptr<VerilatedVcdC> tfp;
std::unique_ptr<VTop> pTop;

void step_and_dump_wave() {
#ifdef HAS_NVBOARD
	nvboard_update();
#endif
	pTop->clock = !pTop->clock;
	pTop->eval();
	pContext->timeInc(1);
	tfp->dump(pContext->time());
}

void sim_init() {
	pContext = std::make_unique<VerilatedContext>();
	tfp = std::make_unique<VerilatedVcdC>();
	pTop = std::make_unique<VTop>("Top");
	pContext->traceEverOn(true);
	pTop->trace(tfp.get(), 0);
	std::filesystem::create_directory(".tmp");
	tfp->open(".tmp/dump.vcd");
#ifdef HAS_NVBOARD
	nvboard_bind_all_pins(pTop.get());
	nvboard_init();
#endif
}

void sim_exit() {
	step_and_dump_wave();
	tfp->close();
}

int main() {
	nvboard::init();
	sim_init();
#ifdef HAS_NVBOARD
	while(!(pTop->io_sw>>15)){
		step_and_dump_wave();
	}
#endif
	sim_exit();
	return 0;
}