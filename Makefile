SHELL := /bin/sh

.PHONY: dev prod build clean

dev:
	@sh ./scripts/run_dev.sh

prod:
	@sh ./scripts/run_prod.sh

build:
	@sh ./scripts/build.sh

clean:
	@sh ./scripts/clean.sh
