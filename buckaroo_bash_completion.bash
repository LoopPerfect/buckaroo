# Loosely based on code by eliben
# https://eli.thegreenplace.net/2013/12/26/adding-bash-completion-for-your-own-tools-an-example-for-pss

_buckaroo_complete()
{
  local cur_word prev_word type_list

  # COMP_WORDS is an array of words in the current command line.
  # COMP_CWORD is the index of the current word (the one the cursor is
  # in). So COMP_WORDS[COMP_CWORD] is the current word; we also record
  # the previous word here, although this specific script doesn't
  # use it yet.
  cur_word="${COMP_WORDS[COMP_CWORD]}"
  prev_word="${COMP_WORDS[COMP_CWORD-1]}"

  # Ask pss to generate a list of types it supports
  # TODO: Fetch commands dynamically from Buckaroo
  commands='init install resolve remove add upgrade version help'
  # commands=`buckaroo show-completions`

  # COMPREPLY is the array of possible completions, generated with
  # the compgen builtin.
  COMPREPLY=( $(compgen -W "${commands}" -- ${cur_word}) )

  return 0
}

# Register _buckaroo_complete to provide completion for the following commands
complete -F _buckaroo_complete buckaroo
