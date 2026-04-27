package app.pairs.asset;

import com.google.gson.JsonObject;

import io.github.libsdl4j.api.render.SDL_Renderer;
import io.github.libsdl4j.api.render.SDL_Texture;
import io.github.libsdl4j.api.render.SdlRender;
import io.github.libsdl4j.api.surface.SDL_Surface;

public class CreateTextureOperation implements AssetOperation {
	private String id;
	private String input;

	@Override
	public String type() {
		return "create-texture";
	}

	@Override
	public void configure(JsonObject item) {
		id(item.get("id").getAsString());
		input(item.get("input").getAsString());
	}

	@Override
	public void process(Context ctx) throws Exception {
		System.out.println("  Creating texture: " + id);
		SDL_Surface surface = ctx.getInput(input);
		SDL_Renderer renderer = AssetManager.instance().renderer();
		SDL_Texture texture = SdlRender.SDL_CreateTextureFromSurface(
			renderer, surface
		);
		System.out.println("  Created texture: " + id);
		ctx.put(id, texture);
	}

	public CreateTextureOperation id(String id) {
		this.id = id;
		return this;
	}

	public CreateTextureOperation input(String input) {
		this.input = input;
		return this;
	}
}
