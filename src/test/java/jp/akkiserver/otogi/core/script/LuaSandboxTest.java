package jp.akkiserver.otogi.core.script;

import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class LuaSandboxTest {
    @Test void noHostCapabilitiesOrAllocationHelpersAreExposed() {
        LuaSandbox sandbox = new LuaSandbox("""
                return { on_use = function(ctx)
                  return io == nil and os == nil and debug == nil and luajava == nil
                     and package == nil and require == nil and load == nil and loadfile == nil
                     and dofile == nil and coroutine == nil and string == nil and table == nil
                     and getmetatable == nil and setmetatable == nil and pcall == nil
                     and collectgarbage == nil and _G == nil
                end }
                """, "test");
        assertTrue(sandbox.invoke("on_use", new LuaTable()).toboolean());
    }

    @Test void stopsInfiniteLoopsAtLoadAndInHooks() {
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            LuaError load = assertThrows(LuaError.class, () -> new LuaSandbox("while true do end", "loop"));
            assertTrue(load.getMessage().contains("budget"));
            LuaSandbox sandbox = new LuaSandbox("return { on_use=function(ctx) while true do end end }", "hook-loop");
            LuaError hook = assertThrows(LuaError.class, () -> sandbox.invoke("on_use", new LuaTable()));
            assertTrue(hook.getMessage().contains("budget"));
        });
    }

    @Test void recursionAndExponentialStringsAreRejected() {
        LuaSandbox sandbox = new LuaSandbox("""
                local function recurse() return 1 + recurse() end
                return { on_use=function(ctx) return recurse() end }
                """, "recursion");
        assertThrows(LuaError.class, () -> sandbox.invoke("on_use", new LuaTable()));
        assertThrows(IllegalArgumentException.class, () -> new LuaSandbox("""
                return { on_use=function(ctx) local s="x"; for i=1,100 do s=s..s end end }
                """, "allocation"));
        LuaSandbox repeat = new LuaSandbox("return { on_use=function(ctx) return string.rep('x', 1000000000) end }", "rep");
        assertThrows(LuaError.class, () -> repeat.invoke("on_use", new LuaTable()));
    }

    @Test void globalStateCannotLeakAcrossPlayersOrCalls() {
        LuaSandbox sandbox = new LuaSandbox("count=0; return { on_use=function(ctx) count=count+1; return count end }", "fresh");
        assertEquals(1, sandbox.invoke("on_use", new LuaTable()).toint());
        assertEquals(1, sandbox.invoke("on_use", new LuaTable()).toint());
    }

    @Test void supportsParametersAndRejectsUnknownHookNames() {
        LuaSandbox sandbox = new LuaSandbox("return { on_use=function(ctx) return math.min(ctx.damage * 2, 10) end }", "math");
        LuaTable context = new LuaTable();
        context.set("damage", LuaValue.valueOf(4));
        assertEquals(8, sandbox.invoke("on_use", context).toint());
        assertThrows(IllegalArgumentException.class, () -> new LuaSandbox("return { on_typo=function() end }", "typo"));
        assertThrows(IllegalArgumentException.class, () -> new LuaSandbox("return { on_use=3 }", "bad-hook"));
    }
}
