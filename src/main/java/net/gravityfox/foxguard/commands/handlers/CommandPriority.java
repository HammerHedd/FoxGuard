/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) gravityfox - https://gravityfox.net/
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.gravityfox.foxguard.commands.handlers;

import com.google.common.collect.ImmutableList;
import net.gravityfox.foxguard.FGManager;
import net.gravityfox.foxguard.commands.FGCommandMainDispatcher;
import net.gravityfox.foxguard.handlers.IHandler;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.CommandCallable;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class CommandPriority implements CommandCallable {

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Texts.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        String[] args = {};
        if (!arguments.isEmpty()) args = arguments.split(" +");
        if (args.length == 0) {
            source.sendMessage(Texts.builder()
                    .append(Texts.of(TextColors.GREEN, "Usage: "))
                    .append(getUsage(source))
                    .build());
            return CommandResult.empty();
        } else {
            int successes = 0;
            int failures = 0;
            List<IHandler> handlers = new LinkedList<>();
            FGCommandMainDispatcher.getInstance().getStateMap().get(source).selectedHandlers.stream().forEach(handlers::add);
            PriorityMachine machine = null;
            for (String arg : args) {
                try {
                    PriorityMachine temp = new PriorityMachine(arg);
                    if(machine == null) machine = temp;
                } catch (NumberFormatException ignored) {
                    IHandler handler = FGManager.getInstance().gethandler(arg);
                    if (handler != null && !handlers.contains(handler)) {
                        handlers.add(handler);
                    } else {
                        failures++;
                    }
                }
            }
            if(machine == null){
                throw new CommandException(Texts.of("You must specify a priority!"));
            }
            for(IHandler handler : handlers){
                handler.setPriority(machine.process(handler.getPriority()));
                successes++;
            }
            if (handlers.size() < 1) throw new CommandException(Texts.of("You must specify at least one Handler!"));
            source.sendMessage(Texts.of(TextColors.GREEN, "Successfully changed priorities with "
                    + successes + " successes and " + failures + " failures!"));
            return CommandResult.builder().successCount(successes).build();
        }
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        return ImmutableList.of();
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxguard.command.modify.objects.handlers.priority");
    }

    @Override
    public Optional<? extends Text> getShortDescription(CommandSource source) {
        return Optional.of(Texts.of("Sets or changes the priority of one or more Handlers."));
    }

    @Override
    public Optional<? extends Text> getHelp(CommandSource source) {
        return Optional.of(Texts.of("This command will modify the priorities of all Handlers currently in your state buffer.\n" +
                "This command takes a minimum of one parameter, which is the priority that all Handlers will be set to.\n" +
                "Prefixing this value with a tilde (\" ~ \") instead increments or decrements the priority of each Handler by that value.\n" +
                "Any arguments after the priority are understood to be additional Handler names not already in your state buffer."));
    }

    @Override
    public Text getUsage(CommandSource source) {
        return Texts.of("priority <priority> [handlers...]");
    }

    private static class PriorityMachine {

        int value;
        boolean delta;

        public PriorityMachine(String arg) throws NumberFormatException {
            if (arg.startsWith("~")) {
                delta = true;
                value = Integer.parseInt(arg.substring(1));
            } else {
                delta = false;
                value = Integer.parseInt(arg);
            }
        }

        public int process(int input) {
            if (delta) return input + value;
            else return value;
        }
    }

}
