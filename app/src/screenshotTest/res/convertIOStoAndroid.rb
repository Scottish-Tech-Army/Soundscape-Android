#!/usr/bin/ruby

# based on https://github.com/tmurakam/cashflow/blob/0a01ac9e0350dfb04979986444244f8daf4cb5a8/android/convertStrings.rb
# support comments and Converter such as "%@", "%d", "%0.1f"...
# in your directory : ./main.rb Localizable.strings

# For Soundscape I ran the following shell script on each file. It converts to UTF-8 where necessary
# and then calls this Ruby script to do the conversion. It strips comments for files other than the
# default language (en-US) where they are all empty.
#
# e.g. ./convert.sh en-US.lproj/Localizable.strings ~/STA/Soundscape-AndroidTest/app/src/main/res/values/strings.xml
#
#        # Convert any UTF-16 files to UTF-8
#        utf16=`file $1 | grep UTF-16`
#        utf8_filename=$1
#        strings_filename=$2
#
#        # We keep the comments in the default strings file
#        strip_comments=true
#        if [[ $strings_filename == *"values/strings.xml" ]]; then
#            strip_comments=false
#        fi
#
#        # The default strings file is UTF-8, the others are UTF-16LE
#        if [ "$utf16" != "" ]; then
#            utf8_filename=$1.utf8
#            iconv -f UTF-16LE -t UTF-8 $1 -o $utf8_filename
#        fi
#
#        # Now convert the files to Android format
#        ~/STA/Soundscape-AndroidTest/app/src/main/res/convertIOStoAndroid.rb --strip-comments=$strip_comments --input_file=$utf8_filename --output_file=$strings_filename
#


require 'optparse'

options = {}
OptionParser.new do |opt|
  opt.on('--input_file INPUTFILE') { |o| options[:input_file] = o }
  opt.on('--output_file OUTPUTFILE') { |o| options[:output_file] = o }
  opt.on('--strip_comments STRIPCOMMENTS') { |o| options[:strip_comments] = o }
end.parse!

printf "Opening input %s\n", options[:input_file]
printf "Writing to %s\n", options[:output_file]
printf "Strip comments set to %s\n", options[:strip_comments]

input_file = File.open(options[:input_file], "r");
output_file = File.open(options[:output_file], "w");
keep_comments = options[:strip_comments] != "true"

output_file.puts "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
output_file.puts "<resources>"

multiple_line_comment = false

input_file.each do |line|
    if (line =~ /\"(.*)\"\s*=\s*\"(.*)\"/)
        name = $1
        value = $2
        
        name.gsub!(/[ .]/, "_")
        
        value.gsub!(/&/, "&amp;")
        value.gsub!(/</, "&lt;")
        
        i = 0
        # convert %@ to %1$s
        value.gsub!(/%([0-9]*.*[@sScCdoxXfeEgabBhH])/) {|s|
        	i += 1
        	match = $1
        	match.gsub!(/@/, "s")
        	"%#{match}"
        }
        
        output_file.puts "  <string name=\"#{name}\">\"#{value}\"</string>"
    # one line comment // The cake is a lie
    # multiple line comment on one line /* The cake is a lie */
    elsif (line =~ /\/\/(.*)/ || line =~ /\/\*(.*)\*\//)
        if (keep_comments) then
            if ($1.include?("-------------------")) then
                output_file.puts  "<!--******************************************************************************-->"
            else
                output_file.puts  "<!--#{$1}-->"
            end
        end
    # multiple line comment (start)
    elsif (line =~ /\/\*(.*)/)
        if (keep_comments) then output_file.puts "<!--#{$1}" end
        multiple_line_comment = true
    # multiple line comment (middle or end)
    elsif (multiple_line_comment)
        if (keep_comments) then output_file.puts "#{$1}-->" end
        #end of the multiple line comment
        if (line =~ /(.*)\*\//)
            multiple_line_comment = false
        end
    elsif (line =~ /\n/)
        if (keep_comments) then output_file.puts line end
    end
end

output_file.puts "</resources>"

