#!/bin/sh

# This script formats the code in the current directory using clang-format.
set -e

# Check if clang-format is installed
if ! clang-format --version | grep -q "version 2[2-9]"; then
	echo "Error: clang-format version 22 or higher is required."
	exit 1
fi

find src -type f \( -name "*.java" \) -print0 | xargs -0 clang-format -i

