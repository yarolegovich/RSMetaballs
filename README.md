A small RenderScript experiment. Be aware of that it's very far from its best application, but I just wanted to try the tool and failed to come up with anything more suitable for it. 

I've tried two different approaches:
 * [The first one](https://github.com/yarolegovich/RSMetaballs/blob/master/app/src/main/java/com/yarolegovich/rsmetaball/rs/RSSurfaceTarget.kt) involves using an Allocation as a SurfaceProducer for a SurfaceView.
 * [The second one](https://github.com/yarolegovich/RSMetaballs/blob/master/app/src/main/java/com/yarolegovich/rsmetaball/rs/RSCanvasTarget.kt) renders to a bitmap and then draws it on a canvas. This approach clearly involves more expensive data transfers, but can be greatly sped up at the cost of reduced image quality.

[Full video.](https://www.youtube.com/watch?v=JdLhtkbEDio)

![](https://j.gifs.com/E9Q41v.gif)

I'd like to note that I'm not going to update this repository.
