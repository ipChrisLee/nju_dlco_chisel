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

#define TRACING

std::unique_ptr<VerilatedContext> pContext;
std::unique_ptr<VerilatedVcdC> tfp;
std::unique_ptr<VTop> pTop;

void step_and_dump_wave() {
#ifdef HAS_NVBOARD
	nvboard_update();
#endif
	pTop->clock = !pTop->clock;
	pTop->eval();
#ifdef TRACING
	pContext->timeInc(1);
	tfp->dump(pContext->time());
#endif
	pTop->clock = !pTop->clock;
	pTop->eval();
#ifdef TRACING
	pContext->timeInc(1);
	tfp->dump(pContext->time());
#endif
}

void sim_init() {
#ifdef TRACING
	pContext = std::make_unique<VerilatedContext>();
	tfp = std::make_unique<VerilatedVcdC>();
	pContext->traceEverOn(true);
#endif
	pTop = std::make_unique<VTop>("Top");
#ifdef TRACING
	pTop->trace(tfp.get(), 0);
	std::filesystem::create_directory(".tmp");
	tfp->open(".tmp/dump.vcd");
#endif
#ifdef HAS_NVBOARD
	nvboard_bind_all_pins(pTop.get());
	nvboard_init();
#endif
	//	reset
	pTop->clock = 0;
	pTop->reset = false;
	step_and_dump_wave();
	pTop->reset = true;
	for (int n = 100; n--;) {
		step_and_dump_wave();
	}
	pTop->reset = false;
}

void sim_exit() {
	step_and_dump_wave();
#ifdef TRACING
	tfp->close();
#endif
}

int main() {
	nvboard::init();
	sim_init();
#ifdef HAS_NVBOARD
	while (!(pTop->io_sw >> 15)) {
		step_and_dump_wave();
	}
#endif
	sim_exit();
	return 0;
}