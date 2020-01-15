package isocity.client;

import static elemental2.dom.DomGlobal.atob;
import static elemental2.dom.DomGlobal.btoa;
import static elemental2.dom.DomGlobal.document;
import static elemental2.dom.DomGlobal.history;
import static elemental2.dom.DomGlobal.location;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import elemental2.core.JsMath;
import elemental2.core.JsString;
import elemental2.dom.BaseRenderingContext2D.FillStyleUnionType;
import elemental2.dom.CanvasRenderingContext2D;
import elemental2.dom.Event;
import elemental2.dom.HTMLCanvasElement;
import elemental2.dom.HTMLElement;
import elemental2.dom.Image;
import elemental2.dom.MouseEvent;
import java.util.stream.Stream;
import jsinterop.base.Js;

/** Original idea from https://github.com/victorqribeiro/isocity */
public class IsoCity implements EntryPoint {
    static final int w = 910, h = 462;
    static final int texWidth = 12, texHeight = 6;
    static final int nTiles = 7;
    static final int tileWidth = 128, tileHeight = 64;

    private int[] tool = { 0, 0 };
    private int[][][] map = {
            { { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 } },
            { { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 } },
            { { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 } },
            { { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 } },
            { { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 } },
            { { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 } },
            { { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 } }
    };
    private Image texture;
    private boolean isPlacing;
    private CanvasRenderingContext2D bg;
    private String activeTool = "";
    private CanvasRenderingContext2D cf;

    @Override public void onModuleLoad() {
        texture = new Image();
        texture.src = bundle.landscape().getSafeUri().asString();
        texture.onload = p0 -> {
            init(); return null;
        };
    }

    void init() {
        HTMLCanvasElement canvas = Js.cast(document.getElementById("bg"));
        canvas.width = 910;
        canvas.height = 666;
        bg = Js.cast(canvas.getContext("2d"));
        bg.translate(w / 2., tileHeight * 2.);

        loadHashState(location.hash.substring(1));

        drawMap();

        HTMLCanvasElement fg = Js.cast(document.getElementById("fg"));
        fg.width = canvas.width;
        fg.height = canvas.height;
        cf = Js.cast(fg.getContext("2d"));
        cf.translate(w / 2., tileHeight * 2.);

        fg.addEventListener("mousemove", e -> viz(Js.cast(e)));
        fg.addEventListener("contextmenu", Event::preventDefault);
        fg.addEventListener("mouseup", e -> unClick());
        fg.addEventListener("mousedown", e -> click(Js.cast(e)));
        fg.addEventListener("touchend", e -> click(Js.cast(e)));
        fg.addEventListener("pointerup", e -> click(Js.cast(e)));

        HTMLElement tools = Js.cast(document.getElementById("tools"));

        var toolCount = 0;
        for (var i = 0; i < texHeight; i++) {
            for (var j = 0; j < texWidth; j++) {
                HTMLElement div = Js.cast(document.createElement("div"));
                div.id = "tool_" + toolCount++;
                div.style.display = "block";
                /* width of 132 instead of 130 = 130 image + 2 border = 132 */
                div.style.backgroundPosition = "-" + (j * 130 + 2) + "px -" + (i * 230) + "px";

                var ij = new int[] { i, j };
                div.addEventListener("click", e -> {
                    tool = ij;
                    if (!activeTool.isEmpty()) {
                        document.getElementById(activeTool).classList.remove(bundle.style().selected());
                    }
                    activeTool = Js.<HTMLElement>cast(e.target).id;
                    document.getElementById(activeTool).classList.add(bundle.style().selected());
                });
                tools.appendChild(div);
            }
        }
    }

    // From https://stackoverflow.com/a/36046727
    static String ToBase64(int... u8) {
        return btoa(JsString.fromCharCode(u8));
    }
    static int[] FromBase64(String str) {
        return Stream.of(atob(str).split("")).filter(c -> !c.isEmpty()).mapToInt(c -> (int) c.charAt(0)).toArray();
    }

    void updateHashState() {
        var u8 = new int[nTiles * nTiles];
        for (int i = 0, c = 0; i < nTiles; i++) {
            for (var j = 0; j < nTiles; j++) {
                u8[c++] = map[i][j][0] * texWidth + map[i][j][1];
            }
        }
        var state = ToBase64(u8);
        history.replaceState(null, null, "#" + state);
    }

    void loadHashState(String state) {
        var u8 = FromBase64(state);
        for (int i = 0, c = 0; i < nTiles; i++) {
            for (var j = 0; j < nTiles; j++) {
                var t = c < u8.length ? u8[c++] : 0.;
                var x = JsMath.trunc(t / texWidth);
                var y = JsMath.trunc(t % texWidth);
                map[i][j] = new int[] { x, y };
            }
        }
    }

    void click(MouseEvent e) {
        var pos = getPosition(e);
        if (pos[0] >= 0 && pos[0] < nTiles && pos[1] >= 0 && pos[1] < nTiles) {
            map[pos[0]][pos[1]][0] = /*(e.which == 3) ? 0 :*/ tool[0];
            map[pos[0]][pos[1]][1] = /*(e.which == 3) ? 0 :*/ tool[1];
            isPlacing = true;

            drawMap();
            cf.clearRect(-w, -h, w * 2, h * 2);
        }
        updateHashState();
    }

    void unClick() {
        if (isPlacing) isPlacing = false;
    }

    void drawMap() {
        bg.clearRect(-w, -h, w * 2, h * 2);
        for (var i = 0; i < nTiles; i++) {
            for (var j = 0; j < nTiles; j++) {
                drawImageTile(bg, i, j, map[i][j][0], map[i][j][1]);
            }
        }
    }

    void drawTile(CanvasRenderingContext2D c, int x, int y, String color) {
        c.save();
        c.translate((y - x) * tileWidth / 2., (x + y) * tileHeight / 2.);
        c.beginPath();
        c.moveTo(0, 0);
        c.lineTo(tileWidth / 2., tileHeight / 2.);
        c.lineTo(0, tileHeight);
        c.lineTo(-tileWidth / 2., tileHeight / 2.);
        c.closePath();
        c.fillStyle = FillStyleUnionType.of(color);
        c.fill();
        c.restore();
    }

    void drawImageTile(CanvasRenderingContext2D c, int x, int y, int i, int j) {
        c.save();
        c.translate((y - x) * tileWidth / 2., (x + y) * tileHeight / 2.);
        j *= 130;
        i *= 230;
        c.drawImage(texture, j, i, 130, 230, -65, -130, 130, 230);
        c.restore();
    }

    int[] getPosition(MouseEvent e) {
        var x = e.offsetX / tileWidth - nTiles / 2.;
        var y = (e.offsetY - tileHeight * 2.) / tileHeight;
        return new int[] { (int) Math.floor(y - x), (int) Math.floor(x + y) };
    }

    void viz(MouseEvent e) {
        if (isPlacing) click(e);
        var pos = getPosition(e);
        cf.clearRect(-w, -h, w * 2, h * 2);
        if (pos[0] >= 0 && pos[0] < nTiles && pos[1] >= 0 && pos[1] < nTiles) {
            drawTile(cf, pos[0], pos[1], "rgba(0,0,0,0.2)");
        }
    }

    interface MyStyle extends CssResource {
        String selected();
    }

    interface MyBundle extends ClientBundle {
        @Source("style.gss") MyStyle style();
        /** Landscape texture from https://opengameart.org/content/isometric-landscape (01 130x66 130x230) */
        @Source("landscape.png") ImageResource landscape();
    }

    static MyBundle bundle = GWT.create(MyBundle.class);

    static {bundle.style().ensureInjected();}
}
