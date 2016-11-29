#!/usr/bin/env bash

# This script only defines common functions used in all the make-xxx-dist scripts.
# You must set the environment variable LOG=logfile.txt to capture the output.

# The directory of this script.
MAKE_DIST_COMMON_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
MAKE_DIST_COMMON_FILE="$SCRIPT_DIR/$(basename ${BASH_SOURCE[0]})"


# ---------------------------------------------------------------------------
# Echo to the $LOG file

function log
{
    echo "$1" | tee -a $LOG
}

# ---------------------------------------------------------------------------
# Run a command and append output to $LOG file which should have already been set.

function run_cmd
{
    local CMD=$1
    #LOG=$2 easier to just use the global $LOG env var set in functions below

    echo " "  >> $LOG
    echo "$CMD" 2>&1 | tee -a $LOG
    eval "$CMD" 2>&1 | tee -a $LOG

    #ret_code=$? this is return code of tee
    local ret_code=${PIPESTATUS[0]}
    if [ $ret_code != 0 ]; then
        printf "Error : [%d] when executing command: '$CMD'\n" $ret_code
        echo "Please see log file: $LOG"
        exit 1
    fi
}

# Change to a directory and exit if it failed
function pushd_cmd
{
    local DIR=$1
    pushd $DIR

    if [ $? -ne 0 ] ; then
        echo "ERROR pushd dir '$DIR'" | tee -a $LOG
    fi
}

# Pop from a directory and exit if the pushd stack was empty
function popd_cmd
{
    popd

    if [ $? -ne 0 ] ; then
        echo "ERROR popd from dir '$PWD'" | tee -a $LOG
    fi
}

# ---------------------------------------------------------------------------
# Read a conf.ini parameter in the form "KEY SEPARATOR VALUE"
# Usage: get_conf_file_property VARIABLE_NAME "KEY_NAME" "=" conf.ini

function get_conf_file_property
{
    local OUTPUT_VAR_NAME=$1
    local KEY=$2
    local SEPARATOR=$3
    local FILENAME=$4

    # NOTE: grep -E does not support \t directly, but it does support the tab
    #       character, so we spit it out using echo.
    if ! grep -E "^[ $(echo -e '\t')]*$KEY[ $(echo -e '\t')]*$SEPARATOR" "$FILENAME" ; then
        echo "ERROR finding conf param '$KEY' in file '$FILENAME', exiting."
        exit 1
    fi

    local VALUE=$(grep -E "^[ $(echo -e '\t')]*$KEY[ $(echo -e '\t')]*$SEPARATOR" "$FILENAME" | cut -d "$SEPARATOR" -f2 | sed -e 's/^ *//' -e 's/ *$//')
    #echo $VALUE

    eval $OUTPUT_VAR_NAME="'$VALUE'"
}

# ---------------------------------------------------------------------------
# Change a conf.ini parameter in the form "KEY = *" to "KEY = VAL" in FILENAME.
# Usage: change_conf_file_property "KEY_NAME" "=" "NEW_VALUE" conf.ini

function change_conf_file_property
{
    local KEY=$1
    local SEPARATOR=$2
    local NEW_VALUE=$3
    local FILENAME=$4

    # NOTE: grep -E does not support \t directly, but it does support the tab
    #       character, so we spit it out using echo.  Also, -P does support it
    #       but -Pz is currently an unsupported combination.
    #       see... http://savannah.gnu.org/forum/forum.php?forum_id=8477
    # Also verify that it was written since sed always returns 0.
    if [[ $KEY == *"\n"* ]]; then
        run_cmd "sed -i \"N;s@\(^[ \t]*${KEY}[ \t]*${SEPARATOR}\).*@\1${NEW_VALUE}@\" $FILENAME"
        local UPDATED_SETTING=$(sed "$!N;/^[ \t]*${KEY}[ \t]*${SEPARATOR}${NEW_VALUE}/p;D" $FILENAME)
        echo "UPDATED_SETTING=$UPDATED_SETTING"
        if [ -z "$UPDATED_SETTING" ]; then
            echo "ERROR: Unable to update configuration setting $KEY to $NEW_VALUE in $FILENAME."
            exit 1
        fi
        # Cannot use the following as currently grep does not support the combination of 
        # -P and -z when searching for beginning or end of line (^ or $).
        #run_cmd "grep -Pzo \"^[ $(echo -e '\t')]*${KEY}[ $(echo -e '\t')]*${SEPARATOR}${NEW_VALUE}\" $FILENAME"
    else
        run_cmd "sed -i \"s@\(^[ \t]*${KEY}[ \t]*${SEPARATOR}\).*@\1${NEW_VALUE}@\" $FILENAME"
        run_cmd "grep -E \"^[ $(echo -e '\t')]*${KEY}[ $(echo -e '\t')]*${SEPARATOR}${NEW_VALUE}\" $FILENAME"
    fi
}

function get_file_attrs
{
    local SEARCH_PATH=$1
    local RESULT_FILE=$2
    local IGNORE_FILES="$(echo ${!3})"
    local CONFIG_FILES="$(echo ${!4})"
    local GHOST_FILES="$(echo ${!5})"

    echo > $RESULT_FILE

    pushd_cmd $SEARCH_PATH
        find . -print0 | while read -d '' -r file; do
            local f=$(echo $file | sed 's@^\./@@g')
            #echo "File attrs: $f"
            if [ -L "$file" ]; then
                # symlinks cannot have attr or dir
                echo "%{prefix}/$f" >> $RESULT_FILE
            elif [ -d "$file" ]; then
                # Is directory
                echo "%dir %attr(0755, %{owner}, %{user}) \"%{prefix}/$f\"" >> $RESULT_FILE
            elif ! echo "$IGNORE_FILES" | grep "$f" > /dev/null ; then
                local FILE_ATTR_PREFIX=""
                if echo "$CONFIG_FILES" | grep "$f" > /dev/null ; then
                    FILE_ATTR_PREFIX='%config(noreplace) '
                elif echo "$GHOST_FILES" | grep "$f" > /dev/null ; then
                    FILE_ATTR_PREFIX='%ghost '
                fi

                # These might be (re)compiled when used, don't let rpm -qV say that they're different, nobody cares.
                if [[ ("$f" == *".pyc") || ("$f" == *".pyo") ]]; then
                    FILE_ATTR_PREFIX='%ghost '
                fi

                if [ -h "$file" ]; then
                    # Symbolic link: warning: Explicit %attr() mode not applicaple to symlink: /opt/gpudb/lib/libjzmq.so.0
                    echo "\"%{prefix}/$f\"" >> $RESULT_FILE
                elif [ -x "$file" ]; then
                    # Is executable
                    echo "$FILE_ATTR_PREFIX %attr(0755, %{owner}, %{user}) \"%{prefix}/$f\"" >> $RESULT_FILE
                else
                    # Is normal file
                    echo "$FILE_ATTR_PREFIX %attr(0644, %{owner}, %{user}) \"%{prefix}/$f\"" >> $RESULT_FILE
                fi
            fi
        done

    popd_cmd

}

# ---------------------------------------------------------------------------
# Get the depenent libs for a specified exe or so.

function get_dependent_libs
{
    local OUTPUT_VAR_NAME=$1
    local EXE_NAME=$2

    local EXE_LIBS=$(ldd $EXE_NAME)
    local EXE_LIBS=$(echo "$EXE_LIBS" | awk '($2 == "=>") && ($3 != "not") && (substr($3,1,3) != "(0x") && (substr($0,length,1) != ":") && ($1" "$2" "$3" "$4 != "not a dynamic executable") {print $3}')
    local EXE_LIBS=$(echo "$EXE_LIBS" | sort | uniq)

    #echo "$EXE_LIBS"

    # Trim out the system libs in /usr/lib* and /lib*, easier to do a positive search.
    local EXE_LIBS=$(echo "$EXE_LIBS" | grep -e "gpudb-core-libs" -e "/home/" -e "/opt/" -e "/usr/local/" | grep -v "opt/sgi")

    eval $OUTPUT_VAR_NAME="'$EXE_LIBS'"
}

# ---------------------------------------------------------------------------
# Turns hardcoded "#!/blah/blah/bin/python" to "#!/usr/bin/env python2.7"
# Leaves "#!/usr/bin/env python" as is.
# Fixes all the shebangs python setup.py hardcoded to the abs path to the python exe used for the install.
# http://stackoverflow.com/questions/1530702/dont-touch-my-shebang
function fix_python_shebang
{
    local PYTHON_FILE="$1"

    local PY_SHEBANG="$(head -n 1 $PYTHON_FILE | grep -e '^#\!' | grep 'bin/python')"

    if [[ "a$PY_SHEBANG" != "a" ]]; then
        echo "Fixing shebang for '$PYTHON_FILE' - was '$PY_SHEBANG'"
        sed -i '1s@.*@#\!/usr/bin/env python2.7@' "$PYTHON_FILE"
    fi
}

# ---------------------------------------------------------------------------
# Fix all the shebangs that python setup.py hardcoded to the python exe used for
# the install.  See above.


function fix_python_shebangs
{
    local DIR="$1"
    pushd_cmd $DIR

        for f in `find . -name '*.py'`; do
            fix_python_shebang "$f"
        done

    popd_cmd
}

# ---------------------------------------------------------------------------
# Get OS and distribution info, e.g. .fc20, .el6, etc

function get_os_dist
{
    local OS=""

    if [ $(which rpmbuild) ]; then
        # returns ".fc20" for example
        OS=$(rpmbuild -E "%{dist}" | sed 's/.centos//g' 2> /dev/null)

        # SLES 11.3 has an older rpmbuild that doesn't have %{dist}, dig out the version number
        if [[ "a$OS" == "a%{dist}" ]]; then
            if [ -f /etc/SuSE-brand ]; then
                OS=".sles$(cat /etc/SuSE-release | grep VERSION | grep -oP "[0-9]+")sp$(cat /etc/SuSE-release | grep PATCHLEVEL | grep -oP "[0-9]*")"
            fi
        fi
    elif [ -f /etc/os-release ]; then
        # Ubuntu for example
        OS=".$(grep -E "^ID=" /etc/os-release | cut -d '=' -f 2)$(grep -E "^VERSION_ID=" /etc/os-release | cut -d '=' -f 2 | sed 's/"//g')"
    fi

    if [[ "a$OS" == "a" ]]; then
        # Print error to everywhere, we need to know when this fails.
        echo "Error - unknown OS! Please install 'rpmbuild' or add appropriate code to $MAKE_DIST_COMMON_FILE get_os_dist()"
        >&2 echo "Error - unknown OS! Please install 'rpmbuild' or add appropriate code to $MAKE_DIST_COMMON_FILE get_os_dist()"
        exit 1
    fi

    echo $OS
}

# ---------------------------------------------------------------------------
# Get GIT repo info

# Echos the git "Build Number" the YYYYMMDDHHMMSS of the last check-in.
# Run this function in the git dir.
function get_git_build_number
{
    # Turn '2016-03-17 22:34:47 -0400' into '20160317223447'
    local GIT_BUILD_DATE="$(git --no-pager log -1 --pretty=format:'%ci')"
    echo $GIT_BUILD_DATE | sed 's/-//g;s/://g' | cut -d' ' -f1,2 | sed 's/ //g'
}

# Check if the git repo has modifications, return code 0 means no modifications
# Run this function in the git dir.
function git_repo_is_not_modified
{
    # This line supposedly confirms that all files are actually unchanged
    # vs someone trying to manually force the git index to believe that a
    # file is unchanged (assume-unchanged).  Not sure if it is necessary in our case, but we
    # are leaving it in for now.
    # See:
    # https://github.com/git/git/commit/b13d44093bac2eb75f37be01f0e369290211472c
    # and
    # http://stackoverflow.com/questions/5143795/how-can-i-check-in-a-bash-script-if-my-local-git-repo-has-changes
    # and
    # https://git-scm.com/docs/git-update-index
    git update-index -q --refresh
    # No local changes && no committed changes that have not yet been pushed (diff upsteam vs HEAD returns no results)
    if git diff-index --quiet HEAD -- && [ -z "$(git log @{u}..)" ] ; then
        return 0
    fi

    return 1
}

function git_repo_is_modified
{
    if git_repo_is_not_modified ; then
        return 1
    fi

    return 0
}

# Echos the get_git_build_number with a trailing 'M' if there are local modifications.
# Run this function in the git dir.
function get_git_build_number_with_modifications
{
    local RESULT=$(get_git_build_number)
    if git_repo_is_modified ; then
        RESULT="${RESULT}M"
    fi
    echo $RESULT
}

# Used to compare build numbers.  Build numbers which have a modified flag
# have a higher precedence than build numbers without, but if both build
# numbers are modified are or not, then the numeric portion of the numbers
# are used.
#
function get_maximum_build_number
{
    local A=$1
    local B=$2
    echo "$A" | grep "M" > /dev/null
    local A_HAS_MODS=$?
    echo "$B" | grep "M" > /dev/null
    local B_HAS_MODS=$?
    local A_NUMBER=$(echo "$A" | sed s/M//g)
    local B_NUMBER=$(echo "$B" | sed s/M//g)

    if [ "$A_HAS_MODS" -ne "$B_HAS_MODS" ]; then
        if [ "$A_HAS_MODS" = 0 ]; then
            echo "$A"
        else
            echo "$B"
        fi
    else
        if [ "$A_NUMBER" -ge "$B_NUMBER" ]; then
            echo "$A"
        else
            echo "$B"
        fi
    fi
}

function get_build_number_has_modifications
{
    local BUILD_NUMBER=$1
    return echo "$BUILD_NUMBER" | grep "M"
}

# Echos the current git branch we are on.
function get_git_branch_name
{
    git rev-parse --abbrev-ref HEAD
}

# Gets the "name" of the git repository - the bitbucket.org/gisfederal/NAME.git
function get_git_repo_name
{
    local OUTPUT_VAR_NAME="$1"
    local GIT_REPO_NAME_RESULT=$(git remote show origin -n | grep "Fetch URL:" | sed -r 's#^.*/(.*)$#\1#' | sed 's#.git$##')
    if [ "$?" -ne 0 ]; then
        >&2 echo "ERROR: Unable to retrieve git repo name of $(pwd)"
        exit 1
    fi
    eval "$OUTPUT_VAR_NAME='$GIT_REPO_NAME_RESULT'"
}

# Gets the hash of the latest commit
function get_git_hash
{
    local OUTPUT_VAR_NAME="$1"
    local GIT_HASH_RESULT=$(git --no-pager log --format=%H -1)
    if [ "$?" -ne 0 ]; then
        >&2 echo "ERROR: Unable to retrieve git hash in $(pwd)"
        exit 1
    fi
    eval $OUTPUT_VAR_NAME="$GIT_HASH_RESULT"
}

# Gets the root folder of the git repository.
function get_git_root_dir
{
    local OUTPUT_VAR_NAME="$1"
    local GIT_ROOT_DIR_RESULT=$(git rev-parse --show-toplevel)
    if [ "$?" -ne 0 ]; then
        >&2 echo "ERROR: Unable to get git root directory of $(pwd)"
        exit 1
    fi
    eval $OUTPUT_VAR_NAME="$GIT_ROOT_DIR_RESULT"
}

# Gets the version stored in the VERSION file of the repository
function get_version
{
    local OUTPUT_VAR_NAME="$1"

    # check local dir first, in case we are in a project where there are
    # multiple VERSION files and/or a non-standard location.
    local VERSION_FILE="$(pwd)/VERSION"
    # If that cannot be found, then look in the root dir for this repo.
    if [ ! -f $VERSION_FILE ]; then
        local GIT_ROOT_DIR=""
        get_git_root_dir "GIT_ROOT_DIR"
        local VERSION_FILE="$GIT_ROOT_DIR/VERSION"
        if [ ! -f $VERSION_FILE ]; then
            >&2 echo "ERROR: Unable to locate version file in $(pwd)"
            exit 1
        fi
    fi
    get_conf_file_property VERSION_INFO_MAJOR_VERSION "MAJOR" "=" $VERSION_FILE > /dev/null
    get_conf_file_property VERSION_INFO_MINOR_VERSION "MINOR" "=" $VERSION_FILE > /dev/null
    get_conf_file_property VERSION_INFO_REVISION "REVISION" "=" $VERSION_FILE > /dev/null

    eval $OUTPUT_VAR_NAME="$VERSION_INFO_MAJOR_VERSION.$VERSION_INFO_MINOR_VERSION.$VERSION_INFO_REVISION"
}

# Appends the version, latest git hash and branch name to the specified file.
function get_version_info
{
    local OUTPUT_VAR_NAME="$1"

    local GET_VERSION_INFO_REPO_NAME=""
    get_git_repo_name "GET_VERSION_INFO_REPO_NAME"

    local GET_VERSION_INFO_VERSION=""
    get_version "GET_VERSION_INFO_VERSION"

    local GET_VERSION_INFO_HASH=""
    get_git_hash "GET_VERSION_INFO_HASH"

    local GET_VERSION_INFO_BRANCH="$(get_git_branch_name)"

    local RESULT="${GET_VERSION_INFO_REPO_NAME}_VERSION=$GET_VERSION_INFO_VERSION-$(get_git_build_number_with_modifications)"
    local RESULT=$(printf "$RESULT\n${GET_VERSION_INFO_REPO_NAME}_HASH=$GET_VERSION_INFO_HASH")
    local RESULT=$(printf "$RESULT\n${GET_VERSION_INFO_REPO_NAME}_BRANCH=$GET_VERSION_INFO_BRANCH\n")

    #>&2 echo "VERSION INFO = $RESULT"
    #>&2 echo "OUTPUT_VAR_NAME=$OUTPUT_VAR_NAME"
    eval "$OUTPUT_VAR_NAME=\"$RESULT\""
}

# Returns 0 (success) if the provided version info contains a line
# that matches *_VERSION=*M
function version_info_contains_modification
{
    if echo "$1" | grep "_VERSION" | grep "M$" > /dev/null; then
        return 0
    fi
    return 1
}

# Returns 0 (success) if the provided version info does not contain a line
# that matches *_VERSION=*M
function version_info_does_not_contain_modification
{
    if version_info_contains_modification "$1"; then
        return 1
    fi
    return 0
}
