SBT_PROJ_NAME=nju_dlco_chisel
CMAKE_PROJ_NAME=nju_dlco_cmake
CMAKE_EXE_NAME=sim
NEW_GIT=y                        # create new git if you define NEW_GIT
SBT_PATH="$(which sbt)"

if [ -z "${SBT_PROJ_NAME}" ] || [ -z "${SBT_PATH}" ] || [ -z "${CMAKE_PROJ_NAME}" ] || [ -z "${CMAKE_EXE_NAME}" ];then
  echo "ERROR!"
  exit 1
fi

CMAKE_TMP=.template/cmake.tmp
SBT_TMP=.template/sbt.tmp

cp .template/CMakeLists.txt.template "${CMAKE_TMP}"
cp .template/build.sbt.template "${SBT_TMP}"

# sbt
sed -i "s|__SBT_PATH|\"""${SBT_PATH}""\"|" "${CMAKE_TMP}"
sed -i "s/__SBT_PROJ_NAME/""${SBT_PROJ_NAME}/" "${SBT_TMP}"

# nvboard
if [ -n "${NVBOARD_HOME}" ];then
  echo "NVBoard found : ""${NVBOARD_HOME}"
  sed -i "s|__NVBOARD_HOME|""${NVBOARD_HOME}""|" "${CMAKE_TMP}"
fi

# cmake
sed -i "s/__CMAKE_PROJ_NAME/""${CMAKE_PROJ_NAME}/" "${CMAKE_TMP}"
sed -i "s/__CMAKE_EXE_NAME/""${CMAKE_EXE_NAME}/" "${CMAKE_TMP}"

mv "${CMAKE_TMP}" CMakeLists.txt
mv "${SBT_TMP}" build.sbt

# git
if [ -n "${NEW_GIT}" ];then
	rm -rf .git
	git init
	sed -i "s/^build.sbt//" .gitignore
	sed -i "s/^CMakeLists.txt//" .gitignore
fi
