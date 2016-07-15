#!/usr/bin/env bash

# This script only defines common functions used in all the make-xxx-dist scripts.
# You must set the environment variable LOG=logfile.txt to capture the output.

# ---------------------------------------------------------------------------
# Echo to the $LOG file

function log
{
    echo $1 | tee -a $LOG
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

    if ! grep -E "^[ \t]*$KEY[ \t]*$SEPARATOR" "$FILENAME" ; then
        echo "ERROR finding conf param '$KEY' in file '$FILENAME', exiting."
        exit 1
    fi

    local VALUE=$(grep -E "^[ \t]*$KEY[ \t]*$SEPARATOR" "$FILENAME" | cut -d "$SEPARATOR" -f2 | sed -e 's/^ *//' -e 's/ *$//')
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

    # Also verify that it was written since sed always returns 0.
    if [[ $KEY == *"\n"* ]]; then
        run_cmd "sed -i \"N;s@\(^[ \t]*${KEY}[ \t]*${SEPARATOR}\).*@\1${NEW_VALUE}@\" $FILENAME"
        run_cmd "grep -Pzo \"^[ \t]*${KEY}[ \t]*${SEPARATOR}${NEW_VALUE}\" $FILENAME"
    else
        run_cmd "sed -i \"s@\(^[ \t]*${KEY}[ \t]*${SEPARATOR}\).*@\1${NEW_VALUE}@\" $FILENAME"
        run_cmd "grep -E \"^[ \t]*${KEY}[ \t]*${SEPARATOR}${NEW_VALUE}\" $FILENAME"
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

function get_dependent_libs()
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

# Echos the git "Build Number" the YYYYMMDDHHMMSS of the last check-in.
# Run this function in the git dir.
function get_git_build_number()
{
    # Turn '2016-03-17 22:34:47 -0400' into '20160317223447'
    local GIT_BUILD_DATE="$(git --no-pager log -1 --pretty=format:'%ci')"
    echo $GIT_BUILD_DATE | sed 's/-//g;s/://g' | cut -d' ' -f1,2 | sed 's/ //g'
}

# Check if the git repo has modifications, return code 0 means no modifications
# Run this function in the git dir.
function git_repo_is_not_modified()
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
    return git diff-index --quiet HEAD -- && [ -z "$(git log @{u}..)" ]
}

# Echos the get_git_build_number with a trailing 'M' if there are local modifications.
# Run this function in the git dir.
function get_git_build_number_with_modifications()
{
    local RESULT=$(get_git_build_number)
    if ! git_repo_is_not_modified ; then
        RESULT="${RESULT}M"
    fi
    echo $RESULT
}

# Echos the current git branch we are on.
function get_git_branch_name()
{
    git rev-parse --abbrev-ref HEAD
}
